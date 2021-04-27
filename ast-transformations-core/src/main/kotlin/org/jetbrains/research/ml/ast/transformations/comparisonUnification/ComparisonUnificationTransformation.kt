package org.jetbrains.research.ml.ast.transformations.comparisonUnification

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyBinaryExpression
import org.jetbrains.research.ml.ast.transformations.Transformation
import org.jetbrains.research.ml.ast.transformations.util.PsiUtil

object ComparisonUnificationTransformation : Transformation() {
    override val key: String = "ComparisonUnification"

    override fun forwardApply(psiTree: PsiElement) {
        val binaryExpressions = PsiTreeUtil.collectElementsOfType(psiTree, PyBinaryExpression::class.java)
        val visitor = ComparisonUnificationVisitor()
        PsiUtil.acceptStatements(psiTree.project, binaryExpressions, visitor)
    }
}
