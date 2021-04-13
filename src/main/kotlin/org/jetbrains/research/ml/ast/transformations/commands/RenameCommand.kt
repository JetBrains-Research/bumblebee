package org.jetbrains.research.ml.ast.transformations.commands

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiNamedElement
import org.jetbrains.research.ml.ast.transformations.anonymization.RenameUtil
import java.util.concurrent.Callable


class RenamablePsiElement(private val psiElement: PsiNamedElement,
                          private val newName: String) {
    private val oldName: String = psiElement.name ?: error("Element ${psiElement.text} does not have name")

    fun redo() = RenameUtil.renameElementDelayed(psiElement, newName)

    fun undo() =  RenameUtil.renameElementDelayed(psiElement, oldName)
}


/**
 * Rename psiElement and all references
 */
object RenameCommand : CommandProvider<RenamablePsiElement, Unit>() {

    override fun redo(input: RenamablePsiElement): Callable<Unit> = Callable { input.redo() }

    override fun undo(input: RenamablePsiElement): Callable<*> = Callable { input.undo() }

}
