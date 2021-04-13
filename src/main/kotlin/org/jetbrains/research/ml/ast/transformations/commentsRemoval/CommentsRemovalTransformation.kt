package org.jetbrains.research.ml.ast.transformations.commentsRemoval

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyStringLiteralExpression
import org.jetbrains.research.ml.ast.transformations.commands.ICommandPerformer
import org.jetbrains.research.ml.ast.transformations.Transformation
import org.jetbrains.research.ml.ast.transformations.util.PsiUtil.acceptStatements

object CommentsRemovalTransformation : Transformation() {
    override val key: String = "CommentsRemoval"

    override fun forwardApply(psiTree: PsiElement, commandPerformer: ICommandPerformer) {
        val comments = PsiTreeUtil.collectElementsOfType(psiTree, PsiComment::class.java)
        val stringLiteralExpressions = PsiTreeUtil.collectElementsOfType(psiTree, PyStringLiteralExpression::class.java)
        val visitor = CommentsRemovalVisitor(commandPerformer)
        acceptStatements(psiTree.project, comments + stringLiteralExpressions, visitor)
        visitor.delayedCommands.forEach { commandPerformer.performCommand(it) }
    }
}

// it's here for now, but will be placed to org.jetbrains.research.ml.ast.transformations.commands.Command as a DeleteCommand
class Siblings(val psiToDelete: PsiElement) {
    //    store ranges
    private var prevSibling: PsiElement? = getNextPsiOnCondition( { it.prevSibling }, { it !is PsiWhiteSpace })
    private var nextSibling: PsiElement? = getNextPsiOnCondition( { it.nextSibling }, { it !is PsiWhiteSpace })
    private var parent: PsiElement = psiToDelete.parent!!
    val psiToAdd: PsiElement = generateSubstitute()

    init {
        DeletedPsiElements.addDeletedElement(this)
    }

//    todo rename all of that !!
    private fun generateSubstitute(): PsiElement {
        val generator = PyElementGenerator.getInstance(psiToDelete.project)
        return generator.createFromText(
            LanguageLevel.forElement(psiToDelete),
            psiToDelete::class.java,
            psiToDelete.text
        )
    }

    fun checkDeletedPsi(deletedPsi: PsiElement, addedPsi: PsiElement) {
        if (prevSibling === deletedPsi) prevSibling = addedPsi
        if (nextSibling === deletedPsi) nextSibling = addedPsi
        if (parent === deletedPsi) parent = addedPsi
    }

    private fun getNextPsiOnCondition(getNextPsi: (PsiElement) -> PsiElement?, condition: (PsiElement) -> Boolean): PsiElement? {
        var nextPsi = getNextPsi(psiToDelete)
        while (nextPsi != null && !condition(nextPsi)) {
            nextPsi = getNextPsi(nextPsi)
        }
        return nextPsi
    }
// 14798, nextSibling is 14765
    fun insertBetweenSiblings() {
        prevSibling?.let { prev ->
            require(prevSibling!!.parent === parent) { "Prev sibling is invalid "}
//           вот в этот момент надо видимо обновлять?
            val addedPsi = parent.addAfter(psiToAdd, prev)
            DeletedPsiElements.updateValue(psiToDelete, addedPsi)
        } ?:
        nextSibling?.let { next ->
            require(nextSibling!!.parent === parent) { "Next sibling is invalid "}
            val addedPsi = parent.addBefore(psiToAdd, next)
            DeletedPsiElements.updateValue(psiToDelete, addedPsi)

        } ?: run {
            val addedPsi = parent.add(psiToDelete)
            DeletedPsiElements.updateValue(psiToDelete, addedPsi)
        }
    }

    // надо чекнуть, куда деваются вайтспейсы?
// 1. написать функцию, которая удаляет промежутки деревьев и удалять соответственно вместе с вайтспейсами, чтобы хранились правильные сиблинги
// [2.] пробовать по-другому искать сиблингов, например, первый сиблинг не вайтспейс

}

object DeletedPsiElements {
    private val psiMap: MutableMap<PsiElement, PsiElement> = mutableMapOf()
    private val oldSiblings = mutableListOf<Siblings>()

    fun addDeletedElement(siblings: Siblings) {
        psiMap[siblings.psiToDelete] = siblings.psiToAdd
        oldSiblings.forEach { it.checkDeletedPsi(siblings.psiToDelete, siblings.psiToAdd) }
        oldSiblings.add(siblings)
    }

    fun updateValue(key: PsiElement, newValue: PsiElement) {
        val oldValue = psiMap[key]!!
        psiMap[key] = newValue
        oldSiblings.forEach { it.checkDeletedPsi(oldValue, newValue) }
    }
}
