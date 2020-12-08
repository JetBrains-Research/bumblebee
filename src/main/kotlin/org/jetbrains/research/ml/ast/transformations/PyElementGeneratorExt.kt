package org.jetbrains.research.ml.ast.transformations

import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyBoolLiteralExpression
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyExpression

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
