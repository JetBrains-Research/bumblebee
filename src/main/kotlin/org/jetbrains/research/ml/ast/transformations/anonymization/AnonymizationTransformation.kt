package org.jetbrains.research.ml.ast.transformations.anonymization

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyFile
import org.jetbrains.research.ml.ast.transformations.MetaDataStorage
import org.jetbrains.research.ml.ast.transformations.Transformation

object AnonymizationTransformation : Transformation() {
    override val key: String = "Anonymization"

    override fun apply(psiTree: PsiElement, metaDataStorage: MetaDataStorage?) {
        val visitor = AnonymizationVisitor(psiTree.containingFile as PyFile)
        psiTree.accept(visitor)
        visitor.performAllRenames()
    }
}
