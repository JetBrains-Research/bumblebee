package org.jetbrains.research.ml.ast.transformations.multiple_target_assignment

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import org.jetbrains.research.ml.ast.transformations.Transformation


class MultipleTargetAssignmentTransformation : Transformation {
    override val metadataKey: String = "MultipleTargetAssigment"
    override fun apply(psiTree: PsiElement, toStoreMetadata: Boolean) {
        val visitor = MultipleTargetAssignmentVisitor()
        WriteCommandAction.runWriteCommandAction(psiTree.project) {
            psiTree.accept(visitor)
        }
    }

    override fun inverseApply(psiTree: PsiElement) {
        TODO("Not yet implemented")
    }
}
