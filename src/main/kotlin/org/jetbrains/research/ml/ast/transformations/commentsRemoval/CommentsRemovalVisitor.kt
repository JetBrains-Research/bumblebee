package org.jetbrains.research.ml.ast.transformations.commentsRemoval

import com.intellij.psi.PsiComment
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStringLiteralExpression
import org.jetbrains.research.ml.ast.transformations.PerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.safePerformCommand
import org.jetbrains.research.ml.ast.transformations.util.PsiUtil.isTripleQuotedString

class CommentsRemovalVisitor(private val commandsStorage: PerformedCommandStorage?) : PyElementVisitor() {

    override fun visitComment(comment: PsiComment) {
        commandsStorage.safePerformCommand({ comment.delete() }, "Delete comment")
        super.visitComment(comment)
    }

    override fun visitPyStringLiteralExpression(node: PyStringLiteralExpression?) {
        if (node != null) {
            if (node.isDocString || node.isTripleQuotedString) {
                commandsStorage.safePerformCommand(
                    { node.parent.delete() },
                    "Delete doc string or triple quoted string"
                )
            }
        }
        super.visitPyStringLiteralExpression(node)
    }
}
