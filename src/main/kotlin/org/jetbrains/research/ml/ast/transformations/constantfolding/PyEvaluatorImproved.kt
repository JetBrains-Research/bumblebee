package org.jetbrains.research.ml.ast.transformations.constantfolding

import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyBinaryExpression
import com.jetbrains.python.psi.PyBoolLiteralExpression
import com.jetbrains.python.psi.PyElementType
import com.jetbrains.python.psi.PyExpression
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
import java.math.BigInteger

class PyEvaluatorImproved {
    // Cache evaluation results to avoid re-evaluating the same expression twice
    private val evaluationResults: MutableMap<PyExpression?, EvaluationResult?> = mutableMapOf()

    // Also cache whether an expression can be proven to be pure (i. e. to not have side-effects)
    private val purityResults: MutableMap<PyExpression?, Boolean> = mutableMapOf()

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

    private fun evaluateBinary(expression: PyBinaryExpression): EvaluationResult? {
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
