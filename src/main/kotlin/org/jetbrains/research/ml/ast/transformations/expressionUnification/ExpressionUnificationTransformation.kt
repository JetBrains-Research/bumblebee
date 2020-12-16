package org.jetbrains.research.ml.ast.transformations.expressionUnification

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyBinaryExpression
import com.jetbrains.python.psi.types.TypeEvalContext
import org.jetbrains.research.ml.ast.transformations.Transformation


class ExpressionUnificationTransformation : Transformation {
    override val metadataKey: String = "ExpressionUnification"

    override fun apply(psiTree: PsiElement, toStoreMetadata: Boolean) {

        val binaryExpressions = PsiTreeUtil.collectElementsOfType(psiTree, PyBinaryExpression::class.java)
        val ancestors = PsiTreeUtil.filterAncestors(binaryExpressions.toTypedArray())
        val typeEvalContext = TypeEvalContext.codeAnalysis(psiTree.project, psiTree as? PsiFile)
        val visitor = ExpressionUnificationVisitor(typeEvalContext)
        for (psiElement in ancestors) {
            psiElement.accept(visitor)
        }
    }

    override fun inverseApply(psiTree: PsiElement) {
        TODO("Not yet implemented")
    }
}
