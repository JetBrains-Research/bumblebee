package org.jetbrains.research.ml.ast.transformations.augmentedAssignment

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyAugAssignmentStatement
import org.jetbrains.research.ml.ast.transformations.IPerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.PerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.Transformation
import org.jetbrains.research.ml.ast.transformations.util.PsiUtil.acceptStatements

object AugmentedAssignmentTransformation : Transformation() {
    override val key: String = "AugmentedAssignment"

    override fun forwardApply(psiTree: PsiElement, commandsStorage: IPerformedCommandStorage?) {
        val augStatements = PsiTreeUtil.collectElementsOfType(psiTree, PyAugAssignmentStatement::class.java)
        val visitor = AugmentedAssignmentVisitor(commandsStorage)
        acceptStatements(psiTree.project, augStatements, visitor)
    }
}
