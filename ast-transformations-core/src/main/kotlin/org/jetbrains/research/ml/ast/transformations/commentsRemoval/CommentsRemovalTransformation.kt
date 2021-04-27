package org.jetbrains.research.ml.ast.transformations.commentsRemoval

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyStringLiteralExpression
import org.jetbrains.research.ml.ast.transformations.Transformation
import org.jetbrains.research.ml.ast.transformations.util.PsiUtil.acceptStatements

object CommentsRemovalTransformation : Transformation() {
    override val key: String = "CommentsRemoval"

    override fun forwardApply(psiTree: PsiElement) {
        val comments = PsiTreeUtil.collectElementsOfType(psiTree, PsiComment::class.java)
        val stringLiteralExpressions = PsiTreeUtil.collectElementsOfType(psiTree, PyStringLiteralExpression::class.java)
        val visitor = CommentsRemovalVisitor()
        acceptStatements(psiTree.project, comments + stringLiteralExpressions, visitor)
    }
}
