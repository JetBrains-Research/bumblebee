package org.jetbrains.research.ml.ast.transformations.commands

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyElementGenerator
import java.util.concurrent.Callable


interface Restorable<T> {
    fun delete()
    fun restore(): T
}


class RestorablePsiElement(val psiElement: PsiElement) : Restorable<PsiElement> {
    private val psiText: String = psiElement.text
    private val psiClass = psiElement.javaClass
    private val psiProject = psiElement.project
    private val isWhitespace = psiElement is PsiWhiteSpace

    override fun delete() {
        WriteCommandAction.runWriteCommandAction(psiProject) {
            psiElement.delete()
        }
    }

    private fun toDelete(): Boolean {
        return psiElement !is PsiWhiteSpace || psiElement.parent != null
    }

    override fun restore(): PsiElement {
        val generator = PyElementGenerator.getInstance(psiProject)
        return generator.createFromText(LanguageLevel.getDefault(), psiClass, psiText)
    }
}



/**
 * Represents a range of adjacent siblings from [startPsi] (inclusive) to [endPsi] (inclusive),
 * siblings are the first (prev, next) siblings that meet a [condition]
 */
open class Range(private val startPsi: PsiElement, private val endPsi: PsiElement, private val condition: (PsiElement) -> Boolean = { true }) {

    val psiElements: List<PsiElement> = getRangeElements()
    open var prevSibling: PsiElement? = findNextSiblingOnCondition(startPsi, { it.prevSibling }, { this.prevSibling = it.newPsi })
        protected set
    open var nextSibling: PsiElement? = findNextSiblingOnCondition(endPsi, { it.nextSibling }, { this.nextSibling = it.newPsi })
        protected set
    open var parent = findParent()
        protected set

    private fun getRangeElements(): List<PsiElement> {
        var currentPsi = startPsi
        val rangePsiElements = mutableListOf(currentPsi)

        while (currentPsi !== endPsi) {
            currentPsi = currentPsi.nextSibling
            rangePsiElements.add(currentPsi)
        }
        return rangePsiElements
    }

    private fun findNextSiblingOnCondition(
        initPsi: PsiElement,
        getSibling: (PsiElement) -> PsiElement?,
        onUpdate: (PsiUpdatesPublisher.UpdatedPsi) -> Unit
    ): PsiElement? {
        println("findSiblingOnCondition")
        var nextPsi = getSibling(initPsi)
        while (nextPsi != null && !condition(nextPsi)) {
            nextPsi = getSibling(nextPsi)
        }
        return nextPsi?.also { PsiUpdatesPublisher.subscribe(it, onUpdate) }
    }

    private fun findParent(): PsiElement {
        return startPsi.parent!!.also { PsiUpdatesPublisher.subscribe(it) { this.parent = it.newPsi } }
    }
}



class RestorableRange(private val range: Range) : Restorable<Unit> {
    val restorablePsiElements = range.psiElements.map { RestorablePsiElement(it) }

    override fun delete() {
        restorablePsiElements.forEach { it.delete() }
    }

    override fun restore() {
        range.prevSibling?.let { restoreWithAnchor(it, { psi, anchor -> range.parent.addAfter(psi, anchor) }) } ?:
        range.nextSibling?.let { restoreWithAnchor(it, { psi, anchor -> range.parent.addBefore(psi, anchor) }) } ?:
        run {
                val prevSibling = restoreAndNotify(restorablePsiElements.first()) { range.parent.add(it) }
                restoreWithAnchor(prevSibling, { psi, anchor -> range.parent.addAfter(psi, anchor) }, 1)
            }
    }

    private fun restoreWithAnchor(
        anchor: PsiElement,
        addToParent: (PsiElement, PsiElement) -> PsiElement,
        dropN: Int = 0
    ) = restorablePsiElements.drop(dropN).fold(anchor, { acc, r ->
        run {
            println("restore with anchor")
            require(acc.parent === range.parent)
            restoreAndNotify(r) { addToParent(it, acc) }
        }
    })


    private fun restoreAndNotify(
        restorablePsi: RestorablePsiElement,
        addToParent: (PsiElement) -> PsiElement
    ): PsiElement {
        val addedPsi = WriteCommandAction.runWriteCommandAction<PsiElement>(restorablePsi.psiElement.project) {
            addToParent(restorablePsi.restore())
        }
        PsiUpdatesPublisher.notify(PsiUpdatesPublisher.UpdatedPsi(restorablePsi.psiElement, addedPsi))
        return addedPsi
    }
}


/**
 * Deletes a range of psiElements (they must be adjacent siblings)
 */
object DeleteCommand : CommandProvider<RestorableRange, Unit>() {

    override fun redo(input: RestorableRange): Callable<Unit> {
        return Callable { input.delete() }
    }

    override fun undo(input: RestorableRange): Callable<*> {
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




fun extendRangeOnCondition(startPsi: PsiElement, endPsi: PsiElement, condition: (PsiElement) -> Boolean): Range {
    fun extendBound(bound: PsiElement, getNextBound: (PsiElement) -> PsiElement?): PsiElement {
        var currentBound = bound
        var nextBound = getNextBound(currentBound)
        while(nextBound != null && condition(nextBound)) {
            currentBound = nextBound
            nextBound = getNextBound(currentBound)
        }
        return currentBound
    }

    val extendedStartPsi = extendBound(startPsi) { it.prevSibling }
    val extendedEndPsi = extendBound(endPsi) { it.nextSibling }
    return Range(extendedStartPsi, extendedEndPsi)
}

/**
 * When PsiElement#delete is called, additional reformatting may be applied that deletes adjacent whitespaces,
 * so it's better to avoid them being prevSibling and nextSibling of a Range. Thus we can include them into the Range.
 * Turned out that we cannot include them in a range since we cannot delete them :(
 */
//fun PsiElement.getWhitespaceRange(): Range = extendRangeOnCondition(this, this) { it is PsiWhiteSpace }
fun PsiElement.getWhitespaceRange() = Range(this, this) { it !is PsiWhiteSpace }
