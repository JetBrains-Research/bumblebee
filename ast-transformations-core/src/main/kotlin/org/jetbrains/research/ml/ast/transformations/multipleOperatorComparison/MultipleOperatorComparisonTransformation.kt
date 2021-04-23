package org.jetbrains.research.ml.ast.transformations.multipleOperatorComparison

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyBinaryExpression
import org.jetbrains.research.ml.ast.transformations.PerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.Transformation
import org.jetbrains.research.ml.ast.transformations.util.PsiUtil

object MultipleOperatorComparisonTransformation : Transformation() {
    override val key: String = "MultipleOperatorComparison"

    override fun forwardApply(psiTree: PsiElement, commandsStorage: PerformedCommandStorage?) {
        val binaryExpressions = PsiTreeUtil.collectElementsOfType(psiTree, PyBinaryExpression::class.java)
        val visitor = MultipleOperatorComparisonVisitor(commandsStorage)
        PsiUtil.acceptStatements(psiTree.project, binaryExpressions, visitor)
    }
}
