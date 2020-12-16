package org.jetbrains.research.ml.ast.transformations.expressionUnification

import com.intellij.openapi.command.WriteCommandAction
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
        val typeEvalContext = TypeEvalContext.userInitiated(psiTree.project, psiTree as? PsiFile)
        val visitor = ExpressionUnificationVisitor(typeEvalContext)
        WriteCommandAction.runWriteCommandAction(psiTree.project) {
            for (psiElement in ancestors) {
                psiElement.accept(visitor)
            }
        }
    }

    override fun inverseApply(psiTree: PsiElement) {
        TODO("Not yet implemented")
    }
}
