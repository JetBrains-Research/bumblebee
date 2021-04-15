package org.jetbrains.research.ml.ast.transformations.commands

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyElementGenerator
import java.util.concurrent.Callable


class ReplacablePsiElement(private var oldPsiElement: PsiElement, private var newPsiElement: PsiElement) {
    private val project = oldPsiElement.project

    init {
        PsiUpdatesPublisher.subscribe(oldPsiElement) { oldPsiElement = it.newPsi }
    }

    fun switch(): PsiElement {
        val validOldPsiElement = generateFromText(oldPsiElement.text)
        val replacedPsiElement =  WriteCommandAction.runWriteCommandAction<PsiElement>(project) {
//          oldPsiElement becomes invalid
            oldPsiElement.replace(newPsiElement)
        }
//      Change newPsiElement to valid old psi
        newPsiElement = validOldPsiElement
//      Notify that oldPsiElement has changed, it also changes oldPsiElement value to the replacedElement, so now our elements are switched
        PsiUpdatesPublisher.notify(PsiUpdatesPublisher.UpdatedPsi(oldPsiElement, replacedPsiElement))
        return replacedPsiElement
    }

    private fun generateFromText(psiText: String): PsiElement {
        val generator = PyElementGenerator.getInstance(project)
        return generator.createFromText(LanguageLevel.getDefault(), PsiElement::class.java, psiText)
    }
}

class ReplaceCommand(oldPsiElement: PsiElement, newPsiElement: PsiElement) : CommandProvider<PsiElement>() {
    private val replacablePsiElement = ReplacablePsiElement(oldPsiElement, newPsiElement)

    override fun redo(): Callable<PsiElement> {
        return Callable { replacablePsiElement.switch() }
    }

    override fun undo(): Callable<*> {
        return Callable { replacablePsiElement.switch() }
    }
}
