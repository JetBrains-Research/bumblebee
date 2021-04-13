package org.jetbrains.research.ml.ast.transformations.commands

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyElementGenerator
import java.util.concurrent.Callable


//class RestorablePsiElement(val psiElement: PsiElement) : Restorable<PsiElement> {
//
//    private val isWhitespace = psiElement is PsiWhiteSpace
//
//    override fun delete() {
//        WriteCommandAction.runWriteCommandAction(psiProject) {
//            psiElement.delete()
//        }
//    }
//
//    private fun toDelete(): Boolean {
//        return psiElement !is PsiWhiteSpace || psiElement.parent != null
//    }
//
//    override fun restore(): PsiElement {
//        val generator = PyElementGenerator.getInstance(psiProject)
//        return generator.createFromText(LanguageLevel.getDefault(), psiClass, psiText)
//    }
//}


/**
 * Represents a range of adjacent siblings from [startPsi] (inclusive) to [endPsi] (inclusive),
 * siblings are the first (prev, next) siblings that meet a [condition]
 */
class RestorablePsiElement(val psiElement: PsiElement, private val condition: (PsiElement) -> Boolean = { true }) {

    private var psiText: String = psiElement.text
    private val psiClass = psiElement.javaClass
    private val psiProject = psiElement.project
    private var prevSibling: PsiElement? = findNextSiblingOnCondition({ it.prevSibling },
        { psiText = it.text + psiText },
        { this.prevSibling = it.newPsi })
    private var nextSibling: PsiElement? = findNextSiblingOnCondition({ it.nextSibling },
        { psiText = psiText + it.text },
        { this.nextSibling = it.newPsi })
    private var parent = findParent()


    //  Todo: accumulate texts
    private fun findNextSiblingOnCondition(
        getSibling: (PsiElement) -> PsiElement?,
        processSibling: (PsiElement) -> Unit,
        onUpdate: (PsiUpdatesPublisher.UpdatedPsi) -> Unit
    ): PsiElement? {
        println("findSiblingOnCondition 1, text: $psiText")
        var nextSibling = getSibling(psiElement)
        while (nextSibling != null && !condition(nextSibling)) {
            processSibling(nextSibling)
            nextSibling = getSibling(nextSibling)
        }
        println("findSiblingOnCondition 2, text: $psiText")

        return nextSibling?.also { PsiUpdatesPublisher.subscribe(it, onUpdate) }
    }

    private fun findParent(): PsiElement {
        return psiElement.parent!!.also { PsiUpdatesPublisher.subscribe(it) { this.parent = it.newPsi } }
    }


    fun delete() {
        WriteCommandAction.runWriteCommandAction(psiProject) {
            psiElement.delete()
        }
    }

    fun restore() {
        prevSibling?.let { prevSibling -> restoreAndNotify { parent.addAfter(it, prevSibling) } }
            ?: nextSibling?.let { nextSibling -> restoreAndNotify { parent.addBefore(it, nextSibling) } }
            ?: restoreAndNotify { parent.add(it) }
    }

    private fun generateFromText(): PsiElement {
        val generator = PyElementGenerator.getInstance(psiProject)
        return generator.createFromText(LanguageLevel.getDefault(), psiClass, psiText)
    }

    private fun restoreAndNotify(addToParent: (PsiElement) -> PsiElement): PsiElement {
        val addedPsi = WriteCommandAction.runWriteCommandAction<PsiElement>(psiElement.project) {
            addToParent(generateFromText())
        }
        PsiUpdatesPublisher.notify(PsiUpdatesPublisher.UpdatedPsi(psiElement, addedPsi))
        return addedPsi
    }
}


/**
 * Deletes a range of psiElements (they must be adjacent siblings)
 */
object DeleteCommand : CommandProvider<RestorablePsiElement, Unit>() {

    override fun redo(input: RestorablePsiElement): Callable<Unit> {
        return Callable { input.delete() }
    }

    override fun undo(input: RestorablePsiElement): Callable<*> {
        return Callable { input.restore() }
    }
}


// Todo: make PsiUpdatesPublisher per PsiFile
object PsiUpdatesPublisher {
    data class UpdatedPsi(val oldPsi: PsiElement, val newPsi: PsiElement)

    /**
     * Stores all actions need to be performed when some Psi has changes from old to new
     */
    private val subscribedPsi: MutableMap<PsiElement, MutableList<(UpdatedPsi) -> Unit>> = hashMapOf()

    /**
     * Subscribes on [psiElement] update event, performs [onUpdate] once it happens
     */
    fun subscribe(psiElement: PsiElement, onUpdate: (UpdatedPsi) -> Unit) {
        subscribedPsi.getOrPut(psiElement, { arrayListOf() }).add(onUpdate)
    }

    /**
     * Notify all subscribers that some Psi is updated, so all stored onUpdates are called
     */
    fun notify(updatedPsi: UpdatedPsi) {
        subscribedPsi[updatedPsi.oldPsi]?.let { it.forEach { onUpdate -> onUpdate(updatedPsi) } }
    }
}


//fun extendRangeOnCondition(startPsi: PsiElement, endPsi: PsiElement, condition: (PsiElement) -> Boolean): RestorablePsiElement {
//    fun extendBound(bound: PsiElement, getNextBound: (PsiElement) -> PsiElement?): PsiElement {
//        var currentBound = bound
//        var nextBound = getNextBound(currentBound)
//        while(nextBound != null && condition(nextBound)) {
//            currentBound = nextBound
//            nextBound = getNextBound(currentBound)
//        }
//        return currentBound
//    }
//
//    val extendedStartPsi = extendBound(startPsi) { it.prevSibling }
//    val extendedEndPsi = extendBound(endPsi) { it.nextSibling }
//    return RestorablePsiElement(extendedStartPsi, extendedEndPsi)
//}

/**
 * When PsiElement#delete is called, additional reformatting may be applied that deletes adjacent whitespaces,
 * so it's better to avoid them being prevSibling and nextSibling of a Range. Thus we can include them into the Range.
 * Turned out that we cannot include them in a range since we cannot delete them :(
 */
//fun PsiElement.getWhitespaceRange(): RestorablePsiElement = extendRangeOnCondition(this, this) { it is PsiWhiteSpace }
fun PsiElement.makeRestorable() = RestorablePsiElement(this) { it !is PsiWhiteSpace }
