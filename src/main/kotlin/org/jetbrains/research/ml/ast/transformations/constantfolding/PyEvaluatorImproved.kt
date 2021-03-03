package org.jetbrains.research.ml.ast.transformations.constantfolding

import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyBinaryExpression
import com.jetbrains.python.psi.PyBoolLiteralExpression
import com.jetbrains.python.psi.PyElementType
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyKeyValueExpression
import com.jetbrains.python.psi.PyListLiteralExpression
import com.jetbrains.python.psi.PyLiteralExpression
import com.jetbrains.python.psi.PyNumericLiteralExpression
import com.jetbrains.python.psi.PyParenthesizedExpression
import com.jetbrains.python.psi.PyPrefixExpression
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.PySequenceExpression
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.PyTupleExpression
import com.jetbrains.python.psi.impl.PyBuiltinCache
import com.jetbrains.python.psi.impl.PyReferenceExpressionImpl
import com.jetbrains.python.psi.types.PyModuleType
import com.jetbrains.python.psi.types.TypeEvalContext
import org.jetbrains.research.ml.ast.transformations.util.PsiUtil.isComment
import java.math.BigInteger
import kotlin.test.fail

class PyEvaluatorImproved(file: PyFile) {
    // Cache evaluation results to avoid re-evaluating the same expression twice
    private val evaluationResults: MutableMap<PyExpression?, PyEvaluationResult?> = mutableMapOf()

    // Also cache whether an expression can be proven to be pure (i. e. to not have side-effects)
    private val purityResults: MutableMap<PyExpression?, Boolean> = mutableMapOf()

    private val typeEvalContext = TypeEvalContext.userInitiated(file.project, file)
    private val builtinsCache = PyBuiltinCache.getInstance(file)
    private val boolType = builtinsCache.boolType ?: failNoSDK()
    private val integerLikeTypes = listOf(builtinsCache.intType ?: failNoSDK(), boolType)

    private fun failNoSDK(): Nothing = error("A working Python SDK is required to use PyEvaluatorImproved")

    interface PyEvaluationResult
    interface PyIntLike : PyEvaluationResult {
        val bigIntegerValue: BigInteger
    }

    data class PyInt(val value: BigInteger) : PyIntLike {
        override val bigIntegerValue: BigInteger
            get() = value

        companion object {
            val ZERO = PyInt(BigInteger.ZERO)
            val ONE = PyInt(BigInteger.ONE)
            val MINUS_ONE = PyInt(BigInteger.ONE.unaryMinus())
        }
    }

    data class PyBool(val value: Boolean) : PyIntLike {
        override val bigIntegerValue: BigInteger
            get() = if (value) BigInteger.ONE else BigInteger.ZERO

        companion object {
            val TRUE = PyBool(true)
            val FALSE = PyBool(false)
        }
    }

    data class PyExpressionResult(val expression: PyExpression) : PyEvaluationResult
    data class PyString(val string: String) : PyEvaluationResult
    data class PySequence(val elements: List<PyExpression>, val kind: PySequenceKind?) : PyEvaluationResult {
        enum class PySequenceKind { LIST, TUPLE }
        companion object {
            fun getKind(expression: PySequenceExpression): PySequenceKind? =
                when (expression) {
                    is PyListLiteralExpression -> PySequenceKind.LIST
                    is PyTupleExpression -> PySequenceKind.TUPLE
                    else -> null
                }
        }
    }

    data class PyOperandSequence(
        val operator: String,
        val evaluatedValue: PyIntLike?,
        val unevaluatedAtoms: List<PossiblyNegatedExpression>
    ) : PyEvaluationResult

    data class PossiblyNegatedExpression(val expression: PyExpression, val negate: Boolean)

    fun evaluate(expression: PyExpression?): PyEvaluationResult? =
        evaluationResults.getOrPut(expression) { evaluateNoCache(expression) }

    private fun evaluateOrGet(expression: PyExpression?): PyEvaluationResult? =
        evaluate(expression) ?: expression?.let { PyExpressionResult(it) }

    private fun evaluateNoCache(expression: PyExpression?): PyEvaluationResult? =
        try {
            when (expression) {
                is PyParenthesizedExpression -> evaluate(expression.containedExpression)
                is PyLiteralExpression -> evaluateLiteral(expression)
                is PySequenceExpression -> evaluateSequence(expression)
                is PyBinaryExpression -> evaluateBinary(expression)
                is PyPrefixExpression -> evaluatePrefix(expression)
                else -> null
            } ?: evaluateAsOperandSequence(expression)
        } catch (_: ArithmeticException) {
            null
        }

    // Indicate if the current expression is a part of the module's expression.
    // For example: if __name__ == '__main__'
    // TODO: should we check all possible instance members or is __name__ enough for us?
    private val PyStringLiteralExpression.isModuleMember: Boolean
        get() = run {
            val moduleMember = this.parent.children.firstOrNull { it is PyReferenceExpression } ?: return@run false
            PyModuleType.getPossibleInstanceMembers().contains((moduleMember as PyReferenceExpressionImpl).name)
        }

    private fun evaluateLiteral(expression: PyLiteralExpression): PyEvaluationResult? =
        when (expression) {
            is PyBoolLiteralExpression -> PyBool(expression.value)
            is PyNumericLiteralExpression ->
                expression.takeIf { it.isIntegerLiteral }?.bigIntegerValue?.let { PyInt(it) }
            is PyStringLiteralExpression -> {
                if (expression.isComment || expression.isModuleMember) {
                    null
                } else {
                    PyString(expression.stringValue)
                }
            }
            else -> null
        }

    private fun evaluateSequence(expression: PySequenceExpression): PyEvaluationResult =
        PySequence(expression.elements.toList(), PySequence.getKind(expression))

    private fun evaluateBinary(expression: PyBinaryExpression): PyEvaluationResult? {
        val lhs = evaluateOrGet(expression.leftExpression) ?: return null
        val rhs = evaluateOrGet(expression.rightExpression) ?: return null
        val operator = expression.operator ?: return null

        if (operator == PyTokenTypes.PLUS) {
            tryConcatenateSequences(lhs, rhs)?.let { return it }
        }

        if (operator in listOf(PyTokenTypes.AND_KEYWORD, PyTokenTypes.OR_KEYWORD)) {
            // True and x === x
            // False or x === x
            evaluateAsPureBoolean(expression.leftExpression)?.let { leftAsPureBool ->
                if (leftAsPureBool && operator == PyTokenTypes.AND_KEYWORD ||
                    !leftAsPureBool && operator == PyTokenTypes.OR_KEYWORD
                ) {
                    return rhs
                }
            }
            // truthy_value and x === truthy_value
            // falsy_value  or  x === falsy_value
            evaluateAsImpureBoolean(expression.leftExpression)?.let { leftAsImpureBool ->
                if (leftAsImpureBool && operator == PyTokenTypes.OR_KEYWORD ||
                    !leftAsImpureBool && operator == PyTokenTypes.AND_KEYWORD
                ) {
                    return lhs
                }
            }
        }

        if (lhs !is PyIntLike || rhs !is PyIntLike) return null
        return evaluateBinaryIntLike(lhs, rhs, operator)
    }

    private fun tryConcatenateSequences(lhs: PyEvaluationResult, rhs: PyEvaluationResult): PyEvaluationResult? {
        if (lhs is PyString && rhs is PyString) {
            return PyString(lhs.string + rhs.string)
        }
        if (lhs is PySequence && rhs is PySequence) {
            val kind = lhs.kind?.takeIf { it == rhs.kind } ?: return null
            return PySequence(lhs.elements + rhs.elements, kind)
        }
        return null
    }

    private fun evaluateAsPureBoolean(expression: PyExpression?): Boolean? =
        asBoolean(evaluate(expression))?.takeIf { canBeProvenPure(expression) }

    private fun evaluateAsImpureBoolean(expression: PyExpression?): Boolean? = asBoolean(evaluate(expression))

    fun canBeProvenPure(expression: PyExpression?): Boolean =
        purityResults.getOrPut(expression) { canBeProvenPureNoCache(expression) }

    private fun canBeProvenPureNoCache(expression: PyExpression?): Boolean =
        when (val result = evaluate(expression)) {
            is PyInt, is PyBool, is PyString -> true
            is PyExpressionResult -> canBeProvenPure(result.expression)
            is PySequence -> result.elements.all { canBeProvenPure(it) }
            else -> when (expression) {
                is PyReferenceExpression -> true
                is PyKeyValueExpression -> canBeProvenPure(expression.key) && canBeProvenPure(expression.value)
                else -> false
            }
        }

    private fun evaluateBinaryIntLike(lhs: PyIntLike, rhs: PyIntLike, operator: PyElementType): PyIntLike? {
        val lhsValue = lhs.bigIntegerValue
        val rhsValue = rhs.bigIntegerValue

        // Operators always returning an int:
        when (operator) {
            PyTokenTypes.PLUS -> lhsValue.plus(rhsValue)
            PyTokenTypes.MINUS -> lhsValue.minus(rhsValue)
            PyTokenTypes.MULT -> lhsValue.multiply(rhsValue)

            PyTokenTypes.FLOORDIV -> pythonDiv(lhsValue, rhsValue)
            PyTokenTypes.PERC -> pythonMod(lhsValue, rhsValue)

            PyTokenTypes.EXP -> smallPow(lhsValue, rhsValue)

            PyTokenTypes.LTLT -> smallShiftLeft(lhsValue, rhsValue)
            PyTokenTypes.GTGT -> smallShiftRight(lhsValue, rhsValue)

            else -> null
        }?.let { return PyInt(it) }

        // Operators always returning a bool ("and"/"or" are handled separately with partial evaluation):
        when (operator) {
            PyTokenTypes.LT -> lhsValue < rhsValue
            PyTokenTypes.LE -> lhsValue <= rhsValue
            PyTokenTypes.GT -> lhsValue > rhsValue
            PyTokenTypes.GE -> lhsValue >= rhsValue
            PyTokenTypes.EQEQ -> lhsValue == rhsValue
            PyTokenTypes.NE -> lhsValue != rhsValue
            else -> null
        }?.let { return PyBool(it) }

        // Operators returning bool iff both operands are bools:
        return when (operator) {
            PyTokenTypes.AND -> lhsValue.and(rhsValue)
            PyTokenTypes.OR -> lhsValue.or(rhsValue)
            PyTokenTypes.XOR -> lhsValue.xor(rhsValue)
            else -> null
        }?.let { PyInt(it).takeUnless { lhs is PyBool && rhs is PyBool } ?: PyBool(it != BigInteger.ZERO) }
    }

    private fun evaluatePrefix(expression: PyPrefixExpression): PyEvaluationResult? {
        val operand = expression.operand ?: return null
        val operator = expression.operator

        if (operator == PyTokenTypes.NOT_KEYWORD) {
            return evaluateAsPureBoolean(operand)?.let { PyBool(!it) }
        }

        val operandValue = (evaluate(operand) as? PyIntLike)?.bigIntegerValue ?: return null
        val result = when (operator) {
            PyTokenTypes.MINUS -> operandValue.negate()
            PyTokenTypes.TILDE -> operandValue.not()
            else -> null
        }
        return result?.let { PyInt(it) }
    }

    private fun evaluateAsOperandSequence(expression: PyExpression?): PyOperandSequence? {
        val operator = when (expression) {
            is PyBinaryExpression -> expression.operator
            is PyPrefixExpression -> expression.operator
            else -> null
        } ?: return null
        val operation = opTokenToCommutativeOperation[operator] ?: return null
        return extractListOfOperandsCommutative(expression, operation)
    }

    private fun extractListOfOperandsCommutative(
        expression: PyExpression?,
        operation: CommutativeOperation
    ): PyOperandSequence? {
        val values = mutableListOf<PyIntLike>()
        val unevaluatedAtoms = mutableListOf<PossiblyNegatedExpression>()
        val isPlus = operation == CommutativeOperation.PLUS

        fun extract(expression: PyExpression?, negate: Boolean): Boolean {
            if (expression == null) return false
            when (expression) {
                is PyParenthesizedExpression -> {
                    return extract(expression.containedExpression, negate)
                }
                is PyBinaryExpression -> {
                    val operator = expression.operator
                    if (operator == operation.opToken || (isPlus && operator == PyTokenTypes.MINUS)) {
                        return extract(expression.leftExpression, negate) &&
                            extract(expression.rightExpression, negate xor (operator == PyTokenTypes.MINUS))
                    }
                }
                is PyPrefixExpression -> {
                    val operator = expression.operator
                    if (isPlus && (operator == PyTokenTypes.PLUS || operator == PyTokenTypes.MINUS)) {
                        return extract(expression.operand, negate xor (operator == PyTokenTypes.MINUS))
                    }
                }
                else -> {
                    (evaluate(expression) as? PyIntLike)?.let { value ->
                        val realValue = if (negate) PyInt(value.bigIntegerValue.unaryMinus()) else value
                        values.add(realValue)
                        return true
                    }
                    val type = typeEvalContext.getType(expression)
                    if (type in integerLikeTypes) {
                        unevaluatedAtoms.add(PossiblyNegatedExpression(expression, negate))
                        return true
                    }
                }
            }
            return false
        }

        return if (extract(expression, false)) {
            assert(unevaluatedAtoms.isNotEmpty()) {
                "An expression with no unevaluated atoms should have been evaluated fully before calling this function"
            }
            val evaluatedValue = values.reduceOrNull { a, b -> evaluateBinaryIntLike(a, b, operation.opToken)!! }

            val unevaluatedIsBool = unevaluatedAtoms.all { typeEvalContext.getType(it.expression) == boolType }
            val operationIsQuasiBoolean = operation.boolIdentity != null
            val multipleUnevaluated = unevaluatedAtoms.size > 1
            val removeValue =
                if (!unevaluatedIsBool || multipleUnevaluated && !operationIsQuasiBoolean) {
                    evaluatedValue?.bigIntegerValue == operation.identity.bigIntegerValue
                } else {
                    operationIsQuasiBoolean && evaluatedValue == operation.boolIdentity
                }

            PyOperandSequence(operation.opText, evaluatedValue?.takeUnless { removeValue }, unevaluatedAtoms)
        } else {
            null
        }
    }

    companion object {
        private val opTokenToCommutativeOperation: Map<PyElementType, CommutativeOperation> =
            CommutativeOperation.values().associateBy { it.opToken } + (PyTokenTypes.MINUS to CommutativeOperation.PLUS)

        private fun asBoolean(result: PyEvaluationResult?) =
            when (result) {
                is PyBool -> result.value
                is PyInt -> result.value != BigInteger.ZERO
                is PyString -> result.string.isNotEmpty()
                is PySequence -> result.elements.isNotEmpty()
                else -> null
            }

        private fun pythonDiv(numerator: BigInteger, denominator: BigInteger): BigInteger =
            pythonDivMod(numerator, denominator, true)

        private fun pythonMod(numerator: BigInteger, denominator: BigInteger): BigInteger =
            pythonDivMod(numerator, denominator, false)

        private fun pythonDivMod(numerator: BigInteger, denominator: BigInteger, returnDiv: Boolean): BigInteger {
            val roundToZeroDivMod = numerator.divideAndRemainder(denominator)
            val fixRounding = numerator.signum() != denominator.signum() && roundToZeroDivMod[1] != BigInteger.ZERO
            return if (returnDiv) {
                roundToZeroDivMod[0].takeUnless { fixRounding } ?: roundToZeroDivMod[0].dec()
            } else {
                roundToZeroDivMod[1].takeUnless { fixRounding } ?: roundToZeroDivMod[1].add(denominator)
            }
        }

        private fun smallPow(base: BigInteger, exp: BigInteger): BigInteger? {
            val baseLong = base.toLong()
            val expLong = exp.toLong()
            if (base.compareTo(baseLong.toBigInteger()) != 0 ||
                exp.compareTo(expLong.toBigInteger()) != 0
            ) {
                return null
            }
            return checkedPow(baseLong, expLong).toBigInteger()
        }

        private fun checkedPow(base: Long, exp: Long): Long {
            if (exp < 0) throw ArithmeticException()
            var currentExp = exp
            var result = 1L
            var currentPowerOfTwo = base
            while (currentExp > 0) {
                if (currentExp % 2 == 1L) result = Math.multiplyExact(result, currentPowerOfTwo)
                currentExp /= 2
                if (currentExp > 0) currentPowerOfTwo = Math.multiplyExact(currentPowerOfTwo, currentPowerOfTwo)
            }
            return result
        }

        private fun smallShiftLeft(number: BigInteger, shift: BigInteger): BigInteger? =
            smallShiftImpl(number, shift, false)

        private fun smallShiftRight(number: BigInteger, shift: BigInteger): BigInteger? =
            smallShiftImpl(number, shift, true)

        private fun smallShiftImpl(number: BigInteger, shift: BigInteger, shiftRight: Boolean): BigInteger? {
            if (shift.abs() > BigInteger.valueOf(32)) return null
            return shift.toInt().takeUnless { it < 0 }?.let { shiftInt ->
                if (shiftRight) number.shiftRight(shiftInt) else number.shiftLeft(shiftInt)
            }
        }

        private enum class CommutativeOperation(
            val identity: PyIntLike,
            val opToken: PyElementType,
            val opText: String,
            val boolIdentity: PyBool?
        ) {
            PLUS(PyInt.ZERO, PyTokenTypes.PLUS, "+", null),
            MULTIPLY(PyInt.ONE, PyTokenTypes.MULT, "*", null),
            BITAND(PyInt.MINUS_ONE, PyTokenTypes.AND, "&", PyBool.TRUE),
            BITOR(PyInt.ZERO, PyTokenTypes.OR, "|", PyBool.FALSE),
            BITXOR(PyInt.ZERO, PyTokenTypes.XOR, "^", PyBool.FALSE)
        }
    }
}
