package org.jetbrains.research.ml.ast.transformations.commands

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.usageView.UsageInfo
import org.jetbrains.research.ml.ast.transformations.anonymization.RenameUtil
import java.util.concurrent.Callable


class RenamablePsiElement(private val psiElement: PsiElement, private val newName: String) {
    private val oldName: String = (psiElement as PsiNamedElement).name ?: error("Element ${psiElement.text} does not have name")
    private val delayedNewRenames = RenameUtil.renameElementDelayed(psiElement, newName)
    private val delayedOldRenames = RenameUtil.renameElementDelayed(psiElement, oldName)


    fun redo() {
        WriteCommandAction.runWriteCommandAction(psiElement.project) {
            delayedNewRenames()
        }
    }

    fun undo() {
        WriteCommandAction.runWriteCommandAction(psiElement.project) {
            delayedOldRenames()
        }
    }
}


/**
 * Rename psiElement and all references
 */
object RenameCommand : CommandProvider<RenamablePsiElement, Unit>() {

    override fun redo(input: RenamablePsiElement): Callable<Unit> = Callable { input.redo() }

    override fun undo(input: RenamablePsiElement): Callable<*> = Callable { input.undo() }

}
