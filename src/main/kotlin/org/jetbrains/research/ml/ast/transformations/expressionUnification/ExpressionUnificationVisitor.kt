package org.jetbrains.research.ml.ast.transformations.expressionUnification

import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyBinaryExpression
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyElementType
import com.jetbrains.python.psi.PyRecursiveElementVisitor
import com.jetbrains.python.psi.types.TypeEvalContext

internal class ExpressionUnificationVisitor(
    private val typeEvalContext: TypeEvalContext
) : PyRecursiveElementVisitor() {
    override fun visitPyBinaryExpression(node: PyBinaryExpression?) {
        if (node != null) {
            handleBinaryExpression(node)
        }
        super.visitPyBinaryExpression(node)
    }

    private var expressionOrder: String? = null

    private fun handleBinaryExpression(node: PyBinaryExpression) {
        val leftVisitor = ExpressionUnificationVisitor(typeEvalContext)
        val rightVisitor = ExpressionUnificationVisitor(typeEvalContext)
        node.leftExpression.accept(leftVisitor)
        node.rightExpression?.accept(rightVisitor)

        val leftExpressionOrder = leftVisitor.expressionOrder ?: node.leftExpression.text
        val rightExpressionOrder = rightVisitor.expressionOrder ?: node.rightExpression?.text ?: ""

        expressionOrder = if (node.canUnify() && leftExpressionOrder > rightExpressionOrder) {
            node.swapExpressions()
            rightExpressionOrder + leftExpressionOrder
        } else {
            leftExpressionOrder + rightExpressionOrder
        }
    }

    private fun PyBinaryExpression.canUnify(): Boolean {
        if (operator?.isCommutative == false) {
            return false
        }

        val type = typeEvalContext.getType(this)
        return type?.name == "int"
    }

    private val PyElementType.isCommutative: Boolean get() = commutativeTokenMap.containsKey(this)

    private fun PyBinaryExpression.swapExpressions() {
        val binOperator = commutativeTokenMap[operator] ?: return
        val left = leftExpression
        val right = rightExpression ?: return
        val generator = PyElementGenerator.getInstance(project)
        val newBinaryExpression = generator.createBinaryExpression(binOperator, right, left)
        replace(newBinaryExpression)
    }

    companion object {
        private val commutativeTokenMap: Map<PyElementType, String> = mapOf(
            PyTokenTypes.PLUS to "+",
            PyTokenTypes.MULT to "*",
            PyTokenTypes.AND to "&",
            PyTokenTypes.OR to "|",
            PyTokenTypes.XOR to "^"
        )
    }
}
