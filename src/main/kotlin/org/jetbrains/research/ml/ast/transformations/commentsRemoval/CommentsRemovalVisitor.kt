package org.jetbrains.research.ml.ast.transformations.commentsRemoval

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStringLiteralExpression
import org.jetbrains.research.ml.ast.transformations.IPerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.safePerformUndoableCommand
import org.jetbrains.research.ml.ast.transformations.util.PsiUtil.isTripleQuotedString

class CommentsRemovalVisitor(private val commandsStorage: IPerformedCommandStorage?) : PyElementVisitor() {

    override fun visitComment(comment: PsiComment) {
        val siblings = Siblings(comment.parent)
        commandsStorage.safePerformUndoableCommand(
            { comment.delete() },
            { siblings.insertCopyBetweenSiblings() },
            "Delete comment"
        )
        super.visitComment(comment)
    }

    override fun visitPyStringLiteralExpression(node: PyStringLiteralExpression) {
        if (node.isDocString || node.isTripleQuotedString) {
            val siblings = Siblings(node.parent)
            commandsStorage.safePerformUndoableCommand(
                { node.parent.delete() },
                { siblings.insertCopyBetweenSiblings() },
                "Delete doc string or triple quoted string"
            )
        }
        super.visitPyStringLiteralExpression(node)
    }


//  there should be a better way.............
    inner class Siblings(psiElement: PsiElement) {
        private val prevSibling = psiElement.prevSibling
        private val nextSibling = psiElement.nextSibling
        private val parent = psiElement.parent!!
        private val copy =
//            psiElement.copy()
        SmartPointerManager.getInstance(psiElement.project).createSmartPsiElementPointer(psiElement).element!!

        fun insertCopyBetweenSiblings() {
            prevSibling?.let { parent.addAfter(it, copy) } ?:
            nextSibling?.let { parent.addBefore(it, copy) } ?:
            parent.add(copy)
        }
    }
}
