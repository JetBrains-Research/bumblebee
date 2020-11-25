package org.jetbrains.research.ml.ast.transformations.anonymization

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyFile
import org.jetbrains.research.ml.ast.transformations.Transformation

object AnonymizationTransformation : Transformation {
    override val metadataKey: String
        get() = TODO("Not yet implemented")

    override fun inverseApply(psiTree: PsiElement) {
        TODO("Not yet implemented")
    }

    override fun apply(psiTree: PsiElement, toStoreMetadata: Boolean) {
        val visitor = AnonymizationVisitor(psiTree.containingFile as PyFile)
        psiTree.accept(visitor)
        visitor.performAllRenames()
    }
}
