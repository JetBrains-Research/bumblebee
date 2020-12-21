package org.jetbrains.research.ml.ast.transformations.augmentedAssignment

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyAugAssignmentStatement
import org.jetbrains.research.ml.ast.transformations.MetaDataStorage
import org.jetbrains.research.ml.ast.transformations.Transformation

object AugmentedAssignmentTransformation : Transformation() {
    override val key: String = "AugmentedAssignment"

    override fun apply(psiTree: PsiElement, metaDataStorage: MetaDataStorage?) {
        val augStatements = PsiTreeUtil.collectElementsOfType(psiTree, PyAugAssignmentStatement::class.java)

        val visitor = AugmentedAssignmentVisitor()
        WriteCommandAction.runWriteCommandAction(psiTree.project) {
            for (statement in augStatements) {
                statement.accept(visitor)
            }
        }
    }
}
