package org.jetbrains.research.ml.ast.transformations.commentsRemoval

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiComment
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStringLiteralExpression
import org.jetbrains.research.ml.ast.transformations.commands.Command
import org.jetbrains.research.ml.ast.transformations.commands.ICommandPerformer
import org.jetbrains.research.ml.ast.transformations.util.PsiUtil.isTripleQuotedString

// Todo: do we need this? see CommentRemovalTransformation#forwardApply
class CommentsRemovalVisitor(private val commandsPerformer: ICommandPerformer) : PyElementVisitor() {

    val comments = ArrayList<PsiComment>()

    override fun visitComment(comment: PsiComment) {
        val siblings = Siblings(comment)

        commandsPerformer.performCommand(
            Command(
                {
                    WriteCommandAction.runWriteCommandAction(comment.project) {
                        comment.delete()
                    }
                },
                {
                    WriteCommandAction.runWriteCommandAction(comment.project) {
                        siblings.insertBetweenSiblings()
                    }
                }, "Delete comment"
            )
        )
        super.visitComment(comment)
    }

    override fun visitPyStringLiteralExpression(node: PyStringLiteralExpression) {
        if (node.isDocString || node.isTripleQuotedString) {
            val siblings = Siblings(node.parent)
            commandsPerformer.performCommand(
                Command(
                    {
//                        WriteCommandAction.runWriteCommandAction(node.project) {
                        node.parent.delete()
//                        }
                    },
                    {
//                        WriteCommandAction.runWriteCommandAction(node.project) {
                        siblings.insertBetweenSiblings()
//                        }
                    },
                    "Delete doc string or triple quoted string"
                )
            )
        }
        super.visitPyStringLiteralExpression(node)
    }
}
