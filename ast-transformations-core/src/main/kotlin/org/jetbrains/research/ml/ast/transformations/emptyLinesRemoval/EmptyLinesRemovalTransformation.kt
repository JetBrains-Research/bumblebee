package org.jetbrains.research.ml.ast.transformations.emptyLinesRemoval

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.research.ml.ast.transformations.PerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.Transformation
import org.jetbrains.research.ml.ast.transformations.safePerformCommand

object EmptyLinesRemovalTransformation : Transformation() {
    override val key: String = "EmptyLinesRemoval"

    override fun forwardApply(psiTree: PsiElement, commandsStorage: PerformedCommandStorage?) {
        val emptyLines = PsiTreeUtil.collectElementsOfType(psiTree, PsiWhiteSpace::class.java)
        emptyLines.forEach {
            WriteCommandAction.runWriteCommandAction(psiTree.project) {
                commandsStorage.safePerformCommand(
                    { it.delete() },
                    "Delete PsiWhiteSpace element"
                )
            }
        }
    }
}
