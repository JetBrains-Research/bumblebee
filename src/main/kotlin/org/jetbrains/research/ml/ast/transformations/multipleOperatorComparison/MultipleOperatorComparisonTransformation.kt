package org.jetbrains.research.ml.ast.transformations.multipleOperatorComparison

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyBinaryExpression
import org.jetbrains.research.ml.ast.transformations.MetaDataStorage
import org.jetbrains.research.ml.ast.transformations.Transformation

object MultipleOperatorComparisonTransformation : Transformation() {
    override val key: String = "MultipleOperatorComparison"

    override fun apply(psiTree: PsiElement, metaDataStorage: MetaDataStorage?) {
        val binaryExpressions = PsiTreeUtil.collectElementsOfType(psiTree, PyBinaryExpression::class.java)
        val visitor = MultipleOperatorComparisonVisitor()
        WriteCommandAction.runWriteCommandAction(psiTree.project) {
            for (expression in binaryExpressions) {
                expression.accept(visitor)
            }
        }
    }
}
