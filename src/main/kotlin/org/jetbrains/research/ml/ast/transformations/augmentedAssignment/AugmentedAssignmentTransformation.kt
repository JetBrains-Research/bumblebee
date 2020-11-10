package org.jetbrains.research.ml.ast.transformations.augmentedAssignment

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import org.jetbrains.research.ml.ast.transformations.Transformation

class AugmentedAssignmentTransformation : Transformation {
    override val metadataKey: String = "AugmentedAssignment"

    override fun apply(psiTree: PsiElement, toStoreMetadata: Boolean) {
        val visitor = AugmentedAssignmentVisitor()
        WriteCommandAction.runWriteCommandAction(psiTree.project) {
            psiTree.accept(visitor)
        }
    }

    override fun inverseApply(psiTree: PsiElement) {
        TODO("Not yet implemented")
    }
}
