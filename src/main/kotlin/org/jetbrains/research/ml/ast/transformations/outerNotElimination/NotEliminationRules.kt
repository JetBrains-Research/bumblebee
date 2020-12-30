package org.jetbrains.research.ml.ast.transformations.outerNotElimination

import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.*
import org.jetbrains.research.ml.ast.transformations.PerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.safePerformCommand

private val PyExpression.isNegationExpression: Boolean
    get() {
        return this is PyPrefixExpression && operator == PyTokenTypes.NOT_KEYWORD
    }

private val PyPrefixExpression.innerOperand: PyExpression?
    get() {
        var inner = this.operand
        while (inner is PyParenthesizedExpression)
            inner = inner.containedExpression

        return inner
    }

private fun PyElementGenerator.createNegationExpression(
    level: LanguageLevel,
    expression: PyExpression
): PyPrefixExpression {
    val newExpr = createExpressionFromText(level, "not x") as PyPrefixExpression
    val myNode = expression.node
    if (myNode != null) {
        newExpr.node.replaceChild(newExpr.operand!!.node, myNode.copyElement())
    }
    return newExpr
}

private fun PyElementGenerator.createParenthesizedExpression(
    level: LanguageLevel,
    expression: PyExpression
): PyParenthesizedExpression {
    val newExpr = createExpressionFromText(level, "(x)") as PyParenthesizedExpression
    val myNode = expression.node
    if (myNode != null) {
        newExpr.node.replaceChild(newExpr.containedExpression!!.node, myNode.copyElement())
    }
    return newExpr
}

internal sealed class NotEliminationRule {
    abstract fun canApply(expression: PyExpression): Boolean

    // Applies the expression. Returns the new expression.
    // In case of an error. `expression` is returned.
    abstract fun apply(expression: PyPrefixExpression, commandsStorage: PerformedCommandStorage?): PyExpression

    fun applyIfNeeded(
        expression: PyPrefixExpression,
        commandsStorage: PerformedCommandStorage?
    ): PyExpression {
        return if (canApply(expression)) {
            apply(expression, commandsStorage)
        } else {
            expression
        }
    }
}

internal abstract class DeMorganNotEliminationRule : NotEliminationRule() {
    override fun canApply(expression: PyExpression): Boolean {
        if (!expression.isNegationExpression) {
            return false
        }
        expression as PyPrefixExpression

        val inner = expression.innerOperand as? PyBinaryExpression
        return inner?.operator == allowedBinaryOperator
    }

    protected abstract val allowedBinaryOperator: PyElementType

    protected abstract val flippedBinaryOperator: String

    override fun apply(expression: PyPrefixExpression, commandsStorage: PerformedCommandStorage?): PyExpression {
        val inner = expression.innerOperand as? PyBinaryExpression ?: return expression
        val generator = PyElementGenerator.getInstance(expression.project)
        val rightExpression = inner.rightExpression ?: return expression
        val myLeftExpression = generator.createNegationExpression(LanguageLevel.PYTHON36, inner.leftExpression)
        val myRightExpression = generator.createNegationExpression(LanguageLevel.PYTHON36, rightExpression)
        val newLeftExpression = CompositeNotEliminationRule.applyIfNeeded(myLeftExpression, commandsStorage)
        val newRightExpression = CompositeNotEliminationRule.applyIfNeeded(myRightExpression, commandsStorage)
        val myExpression = generator.createBinaryExpression(
            flippedBinaryOperator,
            newLeftExpression,
            newRightExpression
        )
        val newExpression = generator.createParenthesizedExpression(LanguageLevel.PYTHON36, myExpression)
        commandsStorage.safePerformCommand({ expression.replace(newExpression) }, "Apply De Morgan's law")
        return newExpression
    }
}

internal object NegationConjunctionRule : DeMorganNotEliminationRule() {
    override val allowedBinaryOperator: PyElementType = PyTokenTypes.AND_KEYWORD
    override val flippedBinaryOperator: String = "or"
}

internal object NegationDisjunctionRule : DeMorganNotEliminationRule() {
    override val allowedBinaryOperator: PyElementType = PyTokenTypes.OR_KEYWORD
    override val flippedBinaryOperator: String = "and"
}

internal object CompositeNotEliminationRule : NotEliminationRule() {
    private val RULES = listOf(NegationConjunctionRule, NegationDisjunctionRule)
    override fun canApply(expression: PyExpression): Boolean = RULES.any { it.canApply(expression) }

    override fun apply(expression: PyPrefixExpression, commandsStorage: PerformedCommandStorage?): PyExpression {
        val rule = RULES.first { it.canApply(expression) }
        return rule.apply(expression, commandsStorage)
    }
}
