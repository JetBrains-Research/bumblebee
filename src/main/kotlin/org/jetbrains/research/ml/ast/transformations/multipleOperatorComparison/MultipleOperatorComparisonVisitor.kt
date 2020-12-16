package org.jetbrains.research.ml.ast.transformations.multipleOperatorComparison

import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyBinaryExpression
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyElementVisitor
import org.jetbrains.research.ml.ast.transformations.PyUtils

internal class MultipleOperatorComparisonVisitor : PyElementVisitor() {
    override fun visitPyBinaryExpression(node: PyBinaryExpression) {
        handleBinaryExpression(node)
        super.visitPyBinaryExpression(node)
    }

    private fun handleBinaryExpression(node: PyBinaryExpression) {
        if (!node.isMultipleOperatorComparison()) {
            return
        }

        val generator = PyElementGenerator.getInstance(node.project)
        val newBinaryExpression = transformMultipleComparisonExpression(node, generator)
        val newBracedExpression = PyUtils.braceExpression(newBinaryExpression)
        node.replace(newBracedExpression)
    }
}

private fun transformMultipleComparisonExpression(
    node: PyBinaryExpression,
    generator: PyElementGenerator
): PyBinaryExpression {
    if (!node.isMultipleOperatorComparison()) {
        return node
    }

    val leftBinaryExpression = node.leftExpression as PyBinaryExpression
    val nodeOperator = node.psiOperator ?: return node
    val newRightExpression = generator.createBinaryExpression(
        nodeOperator.text,
        leftBinaryExpression.rightExpression,
        node.rightExpression
    )

    val newLeftBinaryExpression = transformMultipleComparisonExpression(leftBinaryExpression, generator)

    return generator.createBinaryExpression("and", newLeftBinaryExpression, newRightExpression)
}

private fun PyBinaryExpression.isComparison(): Boolean = PyTokenTypes.COMPARISON_OPERATIONS.contains(operator)

private fun PyBinaryExpression.isMultipleOperatorComparison(): Boolean {
    when (operator) {
        PyTokenTypes.AND_KEYWORD, PyTokenTypes.OR_KEYWORD -> return false
        else -> {
            val leftBinaryExpression = leftExpression as? PyBinaryExpression ?: return false
            return leftBinaryExpression.isComparison() && this.isComparison()
        }
    }
}
