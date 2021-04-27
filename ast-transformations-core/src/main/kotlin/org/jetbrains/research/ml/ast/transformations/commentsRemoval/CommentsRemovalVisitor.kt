package org.jetbrains.research.ml.ast.transformations.commentsRemoval

import com.intellij.psi.PsiComment
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStringLiteralExpression
import org.jetbrains.research.ml.ast.transformations.util.PsiUtil.isTripleQuotedString

class CommentsRemovalVisitor : PyElementVisitor() {

    override fun visitComment(comment: PsiComment) {
        comment.delete()
        super.visitComment(comment)
    }

    override fun visitPyStringLiteralExpression(node: PyStringLiteralExpression) {
        if (node.isDocString || node.isTripleQuotedString) {
            node.parent.delete()
        }
        super.visitPyStringLiteralExpression(node)
    }
}
