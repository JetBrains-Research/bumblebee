package org.jetbrains.research.ml.ast.transformations.commands

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import java.util.concurrent.Callable


class ReplacablePsiElement(private var oldPsiElement: PsiElement, private var newPsiElement: PsiElement) {
    private val project = oldPsiElement.project

    init {
        PsiUpdatesPublisher.subscribe(oldPsiElement) { oldPsiElement = it.newPsi }
    }

    fun switch(): PsiElement {
        val replacedPsiElement =  WriteCommandAction.runWriteCommandAction<PsiElement>(project) {
            oldPsiElement.replace(newPsiElement)
        }
//      Change newPsiElement to old value
        newPsiElement = oldPsiElement
//      Notify that oldPsiElement has changed, it also changes oldPsiElement value to the replacedElement, so now our elements are switched
        PsiUpdatesPublisher.notify(PsiUpdatesPublisher.UpdatedPsi(oldPsiElement, replacedPsiElement))
        return replacedPsiElement
    }
}

class ReplaceCommand(private val replacablePsiElement: ReplacablePsiElement) : CommandProvider<PsiElement>() {
    override fun redo(): Callable<PsiElement> {
        return Callable { replacablePsiElement.switch() }
    }

    override fun undo(): Callable<*> {
        return Callable { replacablePsiElement.switch() }
    }
}
