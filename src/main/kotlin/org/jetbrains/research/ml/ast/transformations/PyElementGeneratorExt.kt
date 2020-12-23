package org.jetbrains.research.ml.ast.transformations

import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyBoolLiteralExpression
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyExpressionStatement
import com.jetbrains.python.psi.PyIfPart
import com.jetbrains.python.psi.PyIfStatement
import com.jetbrains.python.psi.PyPrefixExpression
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.python.psi.PyStatementList

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
    val whitespace = if (operator.last() !in "+-~") " " else ""
    val prefixExpression =
        createExpressionFromText(defaultLanguageLevel, "${operator}${whitespace}1") as PyPrefixExpression
    prefixExpression.operand!!.replace(operand)
    return prefixExpression
}

fun PyElementGenerator.createIfPart(
    condition: PyExpression,
    statements: List<PyStatement>,
    makeElifPart: Boolean = false
): PyIfPart {
    val ifStatement =
        createFromText(defaultLanguageLevel, PyIfStatement::class.java, "if 1:\n\tpass\nelif 2:\n\tpass")
    val ifPart = if (makeElifPart) ifStatement.elifParts[0] else ifStatement.ifPart
    ifPart.condition!!.replace(condition)
    repopulateStatementList(ifPart.statementList, statements)
    return ifPart
}

fun PyElementGenerator.createIfPartFromIfPart(originalPart: PyIfPart, makeElifPart: Boolean = false): PyIfPart =
    createIfPart(originalPart.condition!!, originalPart.statementList.statements.toList(), makeElifPart)

fun PyElementGenerator.createExpressionStatement(expression: PyExpression): PyExpressionStatement {
    val expressionStatement = createFromText(defaultLanguageLevel, PyExpressionStatement::class.java, "1")
    expressionStatement.expression.replace(expression)
    return expressionStatement
}

private fun repopulateStatementList(
    statementList: PyStatementList,
    newStatements: List<PyStatement>
) {
    require(statementList.statements.size == 1) { "The PyStatementList should have exactly 1 element" }
    require(newStatements.isNotEmpty()) { "Empty statement lists are unsupported" }
    var anchor = statementList.statements[0].replace(newStatements[0])
    for (statement in newStatements.drop(1)) {
        anchor = statementList.addAfter(statement, anchor)
    }
}
