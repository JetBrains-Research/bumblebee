package org.jetbrains.research.ml.ast.transformations.emptyLinesRemoval

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import org.jetbrains.research.ml.ast.transformations.PerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.Transformation
import org.jetbrains.research.ml.ast.transformations.safePerformCommand

object EmptyLinesRemovalTransformation : Transformation() {
    override val key: String = "EmptyLinesRemoval"

    override fun forwardApply(psiTree: PsiElement, commandsStorage: PerformedCommandStorage?) {
        val manager = PsiDocumentManager.getInstance(psiTree.project)
        val document = manager.getDocument(psiTree.containingFile)
            ?: error("The document for the PSI file is not found.")
        val emptyLines = PsiTreeUtil.collectElementsOfType(psiTree, PsiWhiteSpace::class.java).filter {
            // Check that PsiWhiteSpace element consists of at least one empty line
            val prevSibLine = document.getLineNumber(it.prevSibling?.endOffset ?: 0)
            val nextSibLine = document.getLineNumber(it.nextSibling?.startOffset ?: it.endOffset)
            nextSibLine - prevSibLine > 1
        }
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
