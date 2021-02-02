package org.jetbrains.research.ml.ast.transformations.outerNotElimination

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyPrefixExpression
import org.jetbrains.research.ml.ast.transformations.PerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.Transformation

object OuterNotEliminationTransformation : Transformation() {
    override val key: String = "OuterNotElimination"

    override fun forwardApply(psiTree: PsiElement, commandsStorage: PerformedCommandStorage?) {
        val prefixExpressions = PsiTreeUtil.collectElementsOfType(psiTree, PyPrefixExpression::class.java)
        WriteCommandAction.runWriteCommandAction(psiTree.project) {
            for (expression in prefixExpressions) {
                CompositeNotEliminationRule.applyIfNeeded(expression, commandsStorage)
            }
        }
    }
}
