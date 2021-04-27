package org.jetbrains.research.ml.ast.transformations.anonymization

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyRecursiveElementVisitor

class AnonymizationVisitor(file: PyFile) : PyRecursiveElementVisitor() {
    private val project = file.project
    private val anonymizer = ElementAnonymizer()

    override fun visitElement(element: PsiElement) {
        anonymizer.registerElement(element)
        super.visitElement(element)
    }

    fun performAllRenames() {
        val renames = anonymizer.getAllRenames().map { RenameUtil.renameElementDelayed(it.first, it.second) }
        for (rename in renames)
            rename()
    }
}
