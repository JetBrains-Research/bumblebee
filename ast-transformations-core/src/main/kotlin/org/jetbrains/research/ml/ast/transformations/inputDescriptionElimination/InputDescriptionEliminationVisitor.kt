package org.jetbrains.research.ml.ast.transformations.inputDescriptionElimination

import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyReferenceExpression

internal class InputDescriptionEliminationVisitor : PyElementVisitor() {
    override fun visitPyCallExpression(node: PyCallExpression) {
        if (node.isInputCall() && node.argumentList?.arguments?.isNotEmpty() == true) {
                    node.argumentList?.arguments?.forEach { it.delete() }
        }
    }
}

private fun PyCallExpression.isInputCall(): Boolean {
    val callee = callee
    if (callee != null && callee is PyReferenceExpression) {
        return callee.text == "input"
    }
    return false
}
