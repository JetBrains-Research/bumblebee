package org.jetbrains.research.ml.ast.transformations.constantfolding

import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyBinaryExpression
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyPrefixExpression
import com.jetbrains.python.psi.impl.PyEvaluator
import java.math.BigInteger
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.isAccessible

class PyEvaluatorImproved : PyEvaluator() {
    private val evaluationResults: MutableMap<PyExpression?, Any?> = mutableMapOf()
    private val purityResults: MutableMap<PyExpression?, Boolean> = mutableMapOf()

    init {
        enableResolve(false)
        setEvaluateKeys(false)
        setEvaluateCollectionItems(false)
    }

    override fun evaluate(expression: PyExpression?): Any? =
        evaluationResults.getOrPut(expression) { evaluateNoCache(expression) }

    private fun evaluateNoCache(expression: PyExpression?): Any? =
        try {
            when (expression) {
                is PyBinaryExpression -> tryEvaluateBinaryHere(expression)
                is PyPrefixExpression -> tryEvaluatePrefixHere(expression)
                else -> null
            } ?: super.evaluate(expression)
        } catch (_: ArithmeticException) {
            null
        }

    private fun evaluateAsPureBoolean(expression: PyExpression?): Boolean? =
        asBoolean(evaluate(expression))?.takeIf { canBeProvenPure(expression) }

    private fun evaluateAsImpureBoolean(expression: PyExpression?): Boolean? = asBoolean(evaluate(expression))

    private fun canBeProvenPure(expression: PyExpression?): Boolean =
        purityResults.getOrPut(expression) { canBeProvenPureNoCache(expression) }

    private fun canBeProvenPureNoCache(expression: PyExpression?): Boolean =
        when (val result = evaluate(expression)) {
            is Boolean -> true
            is Number -> true
            is String -> true
            is PyExpression -> canBeProvenPure(result)
            is Collection<*> -> result.all { canBeProvenPure(it as? PyExpression) }
            is Map<*, *> -> result.all {
                canBeProvenPure(it.key as? PyExpression) && canBeProvenPure(it.value as? PyExpression)
            }
            else -> false
        }

    private fun tryEvaluateBinaryHere(expression: PyBinaryExpression): Any? {
        val lhs = expression.leftExpression ?: return null
        val rhs = expression.rightExpression ?: return null
        val operator = expression.operator ?: return null

        if (operator in listOf(PyTokenTypes.AND_KEYWORD, PyTokenTypes.OR_KEYWORD)) {
            evaluateAsPureBoolean(expression.leftExpression)?.let { leftAsPureBool ->
                if (leftAsPureBool && operator == PyTokenTypes.AND_KEYWORD ||
                    !leftAsPureBool && operator == PyTokenTypes.OR_KEYWORD
                ) {
                    return evaluate(expression.rightExpression) ?: expression.rightExpression
                }
            }
            evaluateAsImpureBoolean(expression.leftExpression)?.let { leftAsImpureBool ->
                if (leftAsImpureBool && operator == PyTokenTypes.OR_KEYWORD ||
                    !leftAsImpureBool && operator == PyTokenTypes.AND_KEYWORD
                ) {
                    return evaluate(expression.leftExpression) ?: expression.leftExpression
                }
            }
        }

        val lhsValue = asNumber(evaluate(lhs))?.let { toBigInteger(it) } ?: return null
        val rhsValue = asNumber(evaluate(rhs))?.let { toBigInteger(it) } ?: return null
        val result = when (operator) {
            PyTokenTypes.FLOORDIV -> pythonDiv(lhsValue, rhsValue)
            PyTokenTypes.PERC -> pythonMod(lhsValue, rhsValue)

            PyTokenTypes.EXP -> smallPow(lhsValue, rhsValue) ?: return null

            PyTokenTypes.AND -> lhsValue.and(rhsValue)
            PyTokenTypes.OR -> lhsValue.or(rhsValue)
            PyTokenTypes.XOR -> lhsValue.xor(rhsValue)

            PyTokenTypes.LTLT -> smallShiftLeft(lhsValue, rhsValue) ?: return null
            PyTokenTypes.GTGT -> smallShiftRight(lhsValue, rhsValue) ?: return null

            else -> return null
        }
        return fromBigInteger(result)
    }

    private fun tryEvaluatePrefixHere(expression: PyPrefixExpression): Any? {
        val operand = expression.operand ?: return null
        val operator = expression.operator

        if (operator == PyTokenTypes.NOT_KEYWORD) {
            return evaluateAsPureBoolean(operand)?.let { !it }
        }

        val operandValue = asNumber(evaluate(operand))?.let { toBigInteger(it) } ?: return null
        val result = when (operator) {
            PyTokenTypes.TILDE -> operandValue.not()
            else -> return null
        }
        return fromBigInteger(result)
    }

    companion object {
        private fun asNumber(result: Any?): Number? =
            when (result) {
                is Number -> result
                is Boolean -> if (result) 1 else 0
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

        private fun asBoolean(value: Any?): Boolean? =
            when (value) {
                is Boolean -> value
                is Number -> value != 0
                is String -> value.isNotEmpty()
                is Collection<*> -> value.isNotEmpty()
                is Map<*, *> -> value.isNotEmpty()
                else -> null
            }

        // We need to "break into" PyEvaluator to reuse its implementation, because its evaluation
        // of binary expressions is lacking.
        private val toBigIntegerMethod =
            PyEvaluator::class.declaredFunctions.find { it.name == "toBigInteger" }!!
                .also { it.isAccessible = true }

        private val fromBigIntegerMethod =
            PyEvaluator::class.declaredFunctions.find { it.name == "fromBigInteger" }!!
                .also { it.isAccessible = true }

        private fun toBigInteger(value: Number): BigInteger = toBigIntegerMethod.call(value) as BigInteger
        private fun fromBigInteger(value: BigInteger): Number = fromBigIntegerMethod.call(value) as Number
    }
}
