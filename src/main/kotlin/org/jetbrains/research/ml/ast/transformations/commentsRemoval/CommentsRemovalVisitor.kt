package org.jetbrains.research.ml.ast.transformations.commentsRemoval

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStringLiteralExpression
import org.jetbrains.research.ml.ast.transformations.IPerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.safePerformCommand
import org.jetbrains.research.ml.ast.transformations.safePerformUndoableCommand
import org.jetbrains.research.ml.ast.transformations.util.PsiUtil.isTripleQuotedString

class CommentsRemovalVisitor(private val commandsStorage: IPerformedCommandStorage?) : PyElementVisitor() {

    override fun visitComment(comment: PsiComment) {
//        commandsStorage.safePerformCommand({ comment.delete() }, "Delete comment")
        commandsStorage.safePerformUndoableCommand({ comment.delete() }, { undoDelete(comment) }, "Delete comment")
        super.visitComment(comment)
    }

    override fun visitPyStringLiteralExpression(node: PyStringLiteralExpression) {
        if (node.isDocString || node.isTripleQuotedString) {

            commandsStorage.safePerformUndoableCommand(

                { node.parent.delete() },
                { undoDelete(node.parent) },
                "Delete doc string or triple quoted string"
            )
//            commandsStorage.safePerformCommand(
//                { node.parent.delete() },
//                "Delete doc string or triple quoted string"
//            )
        }
        super.visitPyStringLiteralExpression(node)
    }

    fun removeFromParent(psiElement: PsiElement) {
        psiElement.parent
    }

    fun undoDelete(psiElement: PsiElement): () -> Unit  {
        val parent = psiElement.parent

//        try naive way just passing the exact same psiElement
//        there should be a way to make it better.......
        return psiElement.prevSibling?.let {
            { parent.addAfter(psiElement, it) }
        } ?: psiElement.nextSibling?.let {
            { parent.addBefore(psiElement, it) }
        } ?: { parent.add(psiElement) }
    }

    fun undoDeleteWithCopy(psiElement: PsiElement): () -> Unit  {
        val parent = psiElement.parent
        val psiElementCopy = psiElement.copy()

//        try naive way just passing the exact same psiElement
//        there should be a way to make it better.......
        return psiElement.prevSibling?.let {
            { parent.addAfter(psiElementCopy, it) }
        } ?: psiElement.nextSibling?.let {
            { parent.addBefore(psiElementCopy, it) }
        } ?: { parent.add(psiElementCopy) }
    }

    fun undoDeleteWithSmartPointer(psiElement: PsiElement): () -> Unit  {
        val parent = psiElement.parent
        val psiElementSmartPointer = SmartPointerManager.getInstance(psiElement.project).createSmartPsiElementPointer(psiElement).element!!

//        try naive way just passing the exact same psiElement
//        there should be a way to make it better.......
        return psiElement.prevSibling?.let {
            { parent.addAfter(psiElementSmartPointer, it) }
        } ?: psiElement.nextSibling?.let {
            { parent.addBefore(psiElementSmartPointer, it) }
        } ?: { parent.add(psiElementSmartPointer) }
    }
}
