package org.jetbrains.research.ml.ast.transformations.multipleTargetAssignment

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyAssignmentStatement
import org.jetbrains.research.ml.ast.transformations.IPerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.PerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.Transformation
import org.jetbrains.research.ml.ast.transformations.util.PsiUtil

object MultipleTargetAssignmentTransformation : Transformation() {
    override val key: String = "MultipleTargetAssigment"

    override fun forwardApply(psiTree: PsiElement, commandsStorage: IPerformedCommandStorage?) {
        val assignments = PsiTreeUtil.collectElementsOfType(psiTree, PyAssignmentStatement::class.java)
        val visitor = MultipleTargetAssignmentVisitor(commandsStorage)
        PsiUtil.acceptStatements(psiTree.project, assignments, visitor)
    }
}
