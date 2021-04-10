package org.jetbrains.research.ml.ast.transformations.multipleTargetAssignment

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyAssignmentStatement
import org.jetbrains.research.ml.ast.transformations.Transformation
import org.jetbrains.research.ml.ast.transformations.commands.ICommandPerformer
import org.jetbrains.research.ml.ast.transformations.util.PsiUtil

object MultipleTargetAssignmentTransformation : Transformation() {
    override val key: String = "MultipleTargetAssigment"

    override fun forwardApply(psiTree: PsiElement, commandPerformer: ICommandPerformer) {
        val assignments = PsiTreeUtil.collectElementsOfType(psiTree, PyAssignmentStatement::class.java)
        val visitor = MultipleTargetAssignmentVisitor(commandPerformer)
        PsiUtil.acceptStatements(psiTree.project, assignments, visitor)
    }
}
