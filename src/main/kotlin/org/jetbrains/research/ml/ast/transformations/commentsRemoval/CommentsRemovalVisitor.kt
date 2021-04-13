package org.jetbrains.research.ml.ast.transformations.commentsRemoval

import com.intellij.psi.PsiComment
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStringLiteralExpression
import org.jetbrains.research.ml.ast.transformations.commands.*
import org.jetbrains.research.ml.ast.transformations.util.PsiUtil.isTripleQuotedString


class CommentsRemovalVisitor(private val commandPerformer: ICommandPerformer) : PyElementVisitor() {
    val delayedCommands = mutableListOf<Command<Unit>>()

    override fun visitComment(comment: PsiComment) {
        val restorablePsiElement = comment.makeRestorable()
        commandPerformer.performCommand(DeleteCommand.getCommand(restorablePsiElement, "Delete comment"))
//        delayedCommands.add(DeleteCommand.getCommand(range, "Delete comment"))
        super.visitComment(comment)
    }

    override fun visitPyStringLiteralExpression(node: PyStringLiteralExpression) {
        if (node.isDocString || node.isTripleQuotedString) {
            val restorablePsiElement = node.parent.makeRestorable()
            commandPerformer.performCommand(DeleteCommand.getCommand(restorablePsiElement, "Delete doc string or triple quoted string"))
//            delayedCommands.add(DeleteCommand.getCommand(range, "Delete doc string or triple quoted string"))
        }
        super.visitPyStringLiteralExpression(node)
    }
}





