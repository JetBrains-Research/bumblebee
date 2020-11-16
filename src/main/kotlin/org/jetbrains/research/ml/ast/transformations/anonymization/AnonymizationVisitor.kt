package org.jetbrains.research.ml.ast.transformations.anonymization

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyRecursiveElementVisitor

class AnonymizationVisitor(file: PyFile) : PyRecursiveElementVisitor() {
    override fun visitElement(element: PsiElement) {
        anonymizer.registerElement(element)
        super.visitElement(element)
    }

    fun performAllRenames() {
        val renames = anonymizer.getAllRenames().map { RenameUtil.renameElementDelayed(it.first, it.second) }
        WriteCommandAction.runWriteCommandAction(project) {
            renames.forEach { it() }
        }
    }

    private val project = file.project
    private val anonymizer = ElementAnonymizer()
}
