package org.jetbrains.research.ml.ast.transformations.augmentedAssignment

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyAugAssignmentStatement
import org.jetbrains.research.ml.ast.transformations.Transformation
import org.jetbrains.research.ml.ast.transformations.commands.ICommandPerformer
import org.jetbrains.research.ml.ast.transformations.util.PsiUtil.acceptStatements

object AugmentedAssignmentTransformation : Transformation() {
    override val key: String = "AugmentedAssignment"

    override fun forwardApply(psiTree: PsiElement, commandPerformer: ICommandPerformer) {
        val augStatements = PsiTreeUtil.collectElementsOfType(psiTree, PyAugAssignmentStatement::class.java)
        val visitor = AugmentedAssignmentVisitor(commandPerformer)
        acceptStatements(psiTree.project, augStatements, visitor)
    }
}
