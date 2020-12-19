package org.jetbrains.research.ml.ast.transformations

import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyBoolLiteralExpression
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyPrefixExpression

// TODO: merge this file with PyUtils?
private val defaultLanguageLevel = LanguageLevel.getDefault()

fun PyElementGenerator.createBoolLiteralExpression(value: Boolean): PyBoolLiteralExpression {
    return createExpressionFromText(defaultLanguageLevel, if (value) "True" else "False") as PyBoolLiteralExpression
}

/**
 * @return The given number as a Python expression.
 * May be a PyNumericLiteralExpression or a PyPrefixExpression (in case of a negative number).
 */
fun PyElementGenerator.createExpressionFromNumber(value: Number): PyExpression {
    return createExpressionFromText(defaultLanguageLevel, value.toString())
}

fun PyElementGenerator.createBinaryOperandList(operator: String, operands: List<PyExpression>): PyExpression {
    require(operands.isNotEmpty()) { "operands list should not be empty" }
    return operands
        .map { PyUtils.braceExpression(it) }
        .reduce { lhs, rhs -> createBinaryExpression(operator, lhs, rhs) }
}

fun PyElementGenerator.createPrefixExpression(operator: String, operand: PyExpression): PyPrefixExpression {
    val prefixExpression = createExpressionFromText(defaultLanguageLevel, "${operator}1") as PyPrefixExpression
    prefixExpression.operand!!.replace(operand)
    return prefixExpression
}
