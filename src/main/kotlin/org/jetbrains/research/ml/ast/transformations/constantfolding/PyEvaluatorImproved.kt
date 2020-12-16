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
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.TypeEvalContext
import java.math.BigInteger
import kotlin.test.fail

class PyEvaluatorImproved(file: PyFile) {
    // Cache evaluation results to avoid re-evaluating the same expression twice
    private val evaluationResults: MutableMap<PyExpression?, EvaluationResult?> = mutableMapOf()

    // Also cache whether an expression can be proven to be pure (i. e. to not have side-effects)
    private val purityResults: MutableMap<PyExpression?, Boolean> = mutableMapOf()

    private val typeEvalContext = TypeEvalContext.userInitiated(file.project, file)
    private val builtinsCache = PyBuiltinCache.getInstance(file)
    private val integerLikeTypes =
        listOf(builtinsCache.intType ?: failNoSDK(), builtinsCache.boolType ?: failNoSDK())
    private val onlyBoolType = listOf(builtinsCache.boolType ?: failNoSDK())

    private fun failNoSDK(): Nothing =
        fail("A working Python SDK is required to use PyEvaluatorImproved")

    interface EvaluationResult
    data class PyInt(val value: BigInteger) : EvaluationResult
    data class PyBool(val value: Boolean) : EvaluationResult
    data class PyExpressionResult(val expression: PyExpression) : EvaluationResult
    data class PyString(val string: String) : EvaluationResult
    data class PySequence(val elements: List<PyExpression>, val kind: PySequenceKind?) : EvaluationResult {
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
        val operator: PyElementType,
        val evaluatedValue: EvaluationResult?,
        val unevaluatedAtoms: List<PossiblyNegatedExpression>
    ) : EvaluationResult

    data class PossiblyNegatedExpression(val expression: PyExpression, val needsUnaryMinus: Boolean)

    fun evaluate(expression: PyExpression?): EvaluationResult? =
        evaluationResults.getOrPut(expression) { evaluateNoCache(expression) }

    private fun evaluateOrGet(expression: PyExpression?): EvaluationResult? =
        evaluate(expression) ?: expression?.let { PyExpressionResult(it) }

    private fun evaluateNoCache(expression: PyExpression?): EvaluationResult? =
        try {
            when (expression) {
                is PyParenthesizedExpression -> evaluate(expression.containedExpression)
                is PyLiteralExpression -> evaluateLiteral(expression)
                is PySequenceExpression -> evaluateSequence(expression)
                is PyBinaryExpression -> evaluateBinary(expression)
                is PyPrefixExpression -> evaluatePrefix(expression)
                else -> null
            }
        } catch (_: ArithmeticException) {
            null
        }

    private fun evaluateLiteral(expression: PyLiteralExpression): EvaluationResult? =
        when (expression) {
            is PyBoolLiteralExpression -> PyBool(expression.value)
            is PyNumericLiteralExpression ->
                expression.takeIf { it.isIntegerLiteral }?.bigIntegerValue?.let { PyInt(it) }
            is PyStringLiteralExpression -> PyString(expression.stringValue)
            else -> null
        }

    private fun evaluateSequence(expression: PySequenceExpression): EvaluationResult =
        PySequence(expression.elements.toList(), PySequence.getKind(expression))

    private fun evaluateBinary(expression: PyBinaryExpression): EvaluationResult? =
        evaluateBinarySimple(expression) ?: evaluateBinaryAsOperandList(expression)

    private fun evaluateBinarySimple(expression: PyBinaryExpression): EvaluationResult? {
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

        val lhsValue = asBigInteger(lhs) ?: return null
        val rhsValue = asBigInteger(rhs) ?: return null
        return tryEvaluateBinaryAsNumber(lhsValue, rhsValue, operator)?.let { PyInt(it) }
            ?: tryEvaluateBinaryAsBoolean(lhsValue, rhsValue, operator)?.let { PyBool(it) }
    }

    private fun tryConcatenateSequences(lhs: EvaluationResult, rhs: EvaluationResult): EvaluationResult? {
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

    private fun canBeProvenPure(expression: PyExpression?): Boolean =
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

    private fun tryEvaluateBinaryAsNumber(lhs: BigInteger, rhs: BigInteger, operator: PyElementType): BigInteger? =
        when (operator) {
            PyTokenTypes.PLUS -> lhs.plus(rhs)
            PyTokenTypes.MINUS -> lhs.minus(rhs)
            PyTokenTypes.MULT -> lhs.multiply(rhs)

            PyTokenTypes.FLOORDIV -> pythonDiv(lhs, rhs)
            PyTokenTypes.PERC -> pythonMod(lhs, rhs)

            PyTokenTypes.EXP -> smallPow(lhs, rhs)

            PyTokenTypes.AND -> lhs.and(rhs)
            PyTokenTypes.OR -> lhs.or(rhs)
            PyTokenTypes.XOR -> lhs.xor(rhs)

            PyTokenTypes.LTLT -> smallShiftLeft(lhs, rhs)
            PyTokenTypes.GTGT -> smallShiftRight(lhs, rhs)

            else -> null
        }

    private fun tryEvaluateBinaryAsBoolean(lhs: BigInteger, rhs: BigInteger, operator: PyElementType): Boolean? =
        when (operator) {
            PyTokenTypes.LT -> lhs < rhs
            PyTokenTypes.LE -> lhs <= rhs
            PyTokenTypes.GT -> lhs > rhs
            PyTokenTypes.GE -> lhs >= rhs
            PyTokenTypes.EQEQ -> lhs == rhs
            PyTokenTypes.NE -> lhs != rhs
            else -> null
        }

    private fun evaluatePrefix(expression: PyPrefixExpression): EvaluationResult? {
        val operand = expression.operand ?: return null
        val operator = expression.operator

        if (operator == PyTokenTypes.NOT_KEYWORD) {
            return evaluateAsPureBoolean(operand)?.let { PyBool(!it) }
        }

        val operandValue = asBigInteger(evaluate(operand)) ?: return null
        val result = when (operator) {
            PyTokenTypes.MINUS -> operandValue.negate()
            PyTokenTypes.TILDE -> operandValue.not()
            else -> null
        }
        return result?.let { PyInt(it) }
    }

    private fun evaluateBinaryAsOperandList(expression: PyBinaryExpression): EvaluationResult? {
        fun presentEvaluatedValue(value: Any?): EvaluationResult? =
            when (value) {
                is BigInteger -> PyInt(value)
                is Boolean -> PyBool(value)
                null -> null
                else -> fail("Acc should be either BigInteger or Boolean")
            }

        fun <Acc> presentSimpleResult(
            operator: PyElementType,
            result: Pair<Acc, List<PyExpression>>,
            initAcc: Acc
        ): EvaluationResult {
            return PyOperandSequence(
                operator,
                presentEvaluatedValue(result.first).takeIf { it != initAcc },
                result.second.map {
                    PossiblyNegatedExpression(it, false)
                })
        }

        val operator = expression.operator ?: return null

        when (operator) {
            PyTokenTypes.MULT -> extractListOfBigIntegerOperandsCommutative(
                expression, PyTokenTypes.MULT, { a, b -> a.multiply(b) }, BigInteger.ONE
            )?.let { it to BigInteger.ONE }
            PyTokenTypes.AND -> extractListOfBigIntegerOperandsCommutative(
                expression, PyTokenTypes.AND, { a, b -> a.and(b) }, BigInteger.ONE.unaryMinus()
            )?.let { it to BigInteger.ONE.unaryMinus() }
            PyTokenTypes.OR -> extractListOfBigIntegerOperandsCommutative(
                expression, PyTokenTypes.OR, { a, b -> a.or(b) }, BigInteger.ZERO
            )?.let { it to BigInteger.ZERO }
            PyTokenTypes.XOR -> extractListOfBigIntegerOperandsCommutative(
                expression, PyTokenTypes.XOR, { a, b -> a.xor(b) }, BigInteger.ZERO
            )?.let { it to BigInteger.ZERO }
            else -> null
        }?.let { return presentSimpleResult(operator, it.first, it.second) }

        when (operator) {
            PyTokenTypes.AND_KEYWORD -> extractListOfBooleanOperandsCommutative(
                expression, PyTokenTypes.AND_KEYWORD, { a, b -> a && b }, true
            )?.let { it to true }
            PyTokenTypes.OR_KEYWORD -> extractListOfBooleanOperandsCommutative(
                expression, PyTokenTypes.OR_KEYWORD, { a, b -> a || b }, false
            )?.let { it to false }
            else -> null
        }?.let { return presentSimpleResult(operator, it.first, it.second) }

        if (expression.operator == PyTokenTypes.PLUS || expression.operator == PyTokenTypes.MINUS) {
            val (evaluatedValue, unevaluatedAtoms) = extractListOfPlusTerms(expression) ?: return null
            return PyOperandSequence(PyTokenTypes.PLUS,
                presentEvaluatedValue(evaluatedValue).takeIf { it != BigInteger.ZERO },
                unevaluatedAtoms.map {
                    PossiblyNegatedExpression(it.expression, it.negate)
                })
        }

        return null
    }

    private data class UnevaluatedAtom(val expression: PyExpression, val negate: Boolean)

    private fun extractListOfPlusTerms(expression: PyExpression?): Pair<BigInteger, List<UnevaluatedAtom>>? =
        extractListOfOperandsCommutativeImpl(
            expression,
            BigInteger.ZERO,
            { a, b -> a.plus(b) },
            { x -> x.unaryMinus() },
            ::evaluateAsBigInteger,
            integerLikeTypes,
            PyTokenTypes.PLUS,
            PyTokenTypes.MINUS,
            PyTokenTypes.PLUS,
            PyTokenTypes.MINUS
        )

    private fun extractListOfBigIntegerOperandsCommutative(
        expression: PyExpression?,
        operator: PyElementType,
        binaryOp: (BigInteger, BigInteger) -> BigInteger,
        initAcc: BigInteger
    ): Pair<BigInteger, List<PyExpression>>? =
        extractListOfOperandsCommutative(
            expression, operator, binaryOp, initAcc, ::evaluateAsBigInteger, integerLikeTypes
        )

    private fun extractListOfBooleanOperandsCommutative(
        expression: PyExpression?,
        operator: PyElementType,
        binaryOp: (Boolean, Boolean) -> Boolean,
        initAcc: Boolean
    ): Pair<Boolean, List<PyExpression>>? =
        extractListOfOperandsCommutative(
            expression, operator, binaryOp, initAcc, ::evaluateAsBooleanNoCast, onlyBoolType
        )

    private fun <Acc> extractListOfOperandsCommutative(
        expression: PyExpression?,
        operator: PyElementType,
        binaryOp: (Acc, Acc) -> Acc,
        initAcc: Acc,
        evaluateAtom: (PyExpression) -> Acc?,
        allowedTypes: List<PyType>
    ): Pair<Acc, List<PyExpression>>? =
        extractListOfOperandsCommutativeImpl(
            expression, initAcc, binaryOp, null, evaluateAtom, allowedTypes, operator,
            null, null, null
        )?.let { (finalAcc, unevaluatedOperands) ->
            Pair(finalAcc, unevaluatedOperands.map { it.expression })
        }

    private fun evaluateAsBigInteger(expression: PyExpression): BigInteger? =
        asBigInteger(evaluate(expression))

    private fun evaluateAsBooleanNoCast(expression: PyExpression): Boolean? =
        (evaluate(expression) as? PyBool)?.value

    private fun <Acc> extractListOfOperandsCommutativeImpl(
        expression: PyExpression?,
        initAcc: Acc,
        binaryOp: (Acc, Acc) -> Acc,
        negateOp: ((Acc) -> Acc)?,
        evaluateAtom: (PyExpression) -> Acc?,
        allowedTypes: List<PyType>,
        binaryPlus: PyElementType,
        binaryMinus: PyElementType?,
        unaryPlus: PyElementType?,
        unaryMinus: PyElementType?
    ): Pair<Acc, List<UnevaluatedAtom>>? {
        var acc = initAcc
        val unevaluatedAtoms = mutableListOf<UnevaluatedAtom>()
        fun extract(expression: PyExpression?, negate: Boolean): Boolean {
            if (expression == null) return false
            when (expression) {
                is PyParenthesizedExpression -> extract(expression.containedExpression, negate)
                is PyBinaryExpression -> {
                    val operator = expression.operator
                    if (operator == binaryMinus || operator == binaryPlus) {
                        return extract(expression.leftExpression, negate) &&
                            extract(expression.rightExpression, negate xor (operator == binaryMinus))
                    }
                }
                is PyPrefixExpression -> {
                    val operator = expression.operator
                    if (operator == unaryMinus || operator == unaryPlus) {
                        return extract(expression.operand, negate xor (operator == unaryMinus))
                    }
                }
                else -> {
                    evaluateAtom(expression)?.let { value ->
                        val realValue = if (negate) negateOp!!(value) else value
                        acc = binaryOp(acc, realValue)
                        return true
                    }
                    val type = typeEvalContext.getType(expression)
                    if (type in allowedTypes) {
                        unevaluatedAtoms.add(UnevaluatedAtom(expression, negate))
                        return true
                    }
                }
            }
            return false
        }
        return if (extract(expression, false)) {
            Pair(acc, unevaluatedAtoms)
        } else {
            null
        }
    }

    companion object {
        private fun asBigInteger(result: EvaluationResult?) =
            when (result) {
                is PyInt -> result.value
                is PyBool -> if (result.value) BigInteger.ONE else BigInteger.ZERO
                else -> null
            }

        private fun asBoolean(result: EvaluationResult?) =
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
    }
}
