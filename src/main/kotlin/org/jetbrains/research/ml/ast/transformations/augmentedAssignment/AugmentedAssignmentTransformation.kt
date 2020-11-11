package org.jetbrains.research.ml.ast.transformations.augmentedAssignment

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyAugAssignmentStatement
import org.jetbrains.research.ml.ast.transformations.Transformation

class AugmentedAssignmentTransformation : Transformation {
    override val metadataKey: String = "AugmentedAssignment"

    override fun apply(psiTree: PsiElement, toStoreMetadata: Boolean) {
        val augStatements = PsiTreeUtil.collectElementsOfType(psiTree, PyAugAssignmentStatement::class.java)

        val visitor = AugmentedAssignmentVisitor()
        WriteCommandAction.runWriteCommandAction(psiTree.project) {
            for (statement in augStatements) {
                statement.accept(visitor)
            }
        }
    }

    override fun inverseApply(psiTree: PsiElement) {
        TODO("Not yet implemented")
    }
}
