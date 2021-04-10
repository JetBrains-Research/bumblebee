package org.jetbrains.research.ml.ast.transformations.expressionUnification

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyBinaryExpression
import com.jetbrains.python.psi.types.TypeEvalContext
import org.jetbrains.research.ml.ast.transformations.Transformation
import org.jetbrains.research.ml.ast.transformations.commands.ICommandPerformer

object ExpressionUnificationTransformation : Transformation() {
    override val key: String = "ExpressionUnification"

    override fun forwardApply(psiTree: PsiElement, commandPerformer: ICommandPerformer) {
        val binaryExpressions = PsiTreeUtil.collectElementsOfType(psiTree, PyBinaryExpression::class.java)
        val ancestors = PsiTreeUtil.filterAncestors(binaryExpressions.toTypedArray())
        val typeEvalContext = TypeEvalContext.userInitiated(psiTree.project, psiTree as? PsiFile)
        val visitor = ExpressionUnificationVisitor(typeEvalContext, commandPerformer)
        val correctionVisitor = CorrectToLeftAssociativity(commandPerformer)
        WriteCommandAction.runWriteCommandAction(psiTree.project) {
            for (psiElement in ancestors) {
                psiElement.accept(visitor)
            }
            psiTree.accept(correctionVisitor)
        }
    }
}
