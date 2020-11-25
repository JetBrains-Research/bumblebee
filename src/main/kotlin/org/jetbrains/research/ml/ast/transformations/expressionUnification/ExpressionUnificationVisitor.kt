package org.jetbrains.research.ml.ast.transformations.expressionUnification

import com.intellij.psi.tree.TokenSet
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.codeInsight.intentions.PyTypeHintGenerationUtil
import com.jetbrains.python.psi.PyBinaryExpression
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

    private fun handleBinaryExpression(node: PyBinaryExpression) {
//        node.leftExpression.accept(this)
//        node.rightExpression?.accept(this)
        val flag = node.canUnify()

//        PyTypeHintGenerationUtil.insertStandaloneAttributeTypeComment()
    }

    private fun PyBinaryExpression.canUnify(): Boolean {
        if (operator?.isCommutative == false)
            return false

        val type = typeEvalContext.getType(this)
        return false
    }
}

private val commutativeTokenSet = TokenSet.create(
    PyTokenTypes.PLUS,
    PyTokenTypes.MULT,
    PyTokenTypes.AND,
    PyTokenTypes.OR,
    PyTokenTypes.XOR
)

private val PyElementType.isCommutative: Boolean get() = commutativeTokenSet.contains(this)
