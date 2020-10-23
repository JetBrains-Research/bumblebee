package org.jetbrains.research.ml.ast.transformations.deadcode

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyRecursiveElementVisitor
import org.jetbrains.research.ml.ast.storage.MetaDataStorage

internal class DeadCodeRemovalInverseVisitor(private val pyGenerator: PyElementGenerator, private val storage: MetaDataStorage) :
    PyRecursiveElementVisitor() {
    override fun visitElement(element: PsiElement) {
        val unreachableElementTexts = storage.getMetaData(element, DeadCodeRemovalStorageKeys.NODE)
        val unreachableElements = unreachableElementTexts?.map {
            pyGenerator.createFromText(
                LanguageLevel.PYTHON36,
                PsiElement::class.java,
                it
            )
        }
        if (unreachableElements != null) {
            for (unreachable in unreachableElements) {
                element.add(unreachable)
            }
        }
        super.visitElement(element)
    }
}
