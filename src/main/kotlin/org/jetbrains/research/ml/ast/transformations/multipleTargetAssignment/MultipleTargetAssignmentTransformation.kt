package org.jetbrains.research.ml.ast.transformations.multipleTargetAssignment

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyAssignmentStatement
import org.jetbrains.research.ml.ast.transformations.MetaDataStorage
import org.jetbrains.research.ml.ast.transformations.Transformation

object MultipleTargetAssignmentTransformation : Transformation() {
    override val key: String = "MultipleTargetAssigment"

    override fun apply(psiTree: PsiElement, metaDataStorage: MetaDataStorage?) {
        val assignments = PsiTreeUtil.collectElementsOfType(psiTree, PyAssignmentStatement::class.java)
        val visitor = MultipleTargetAssignmentVisitor()
        WriteCommandAction.runWriteCommandAction(psiTree.project) {
            for (assignment in assignments) {
                assignment.accept(visitor)
            }
        }
    }
}
