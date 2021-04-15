package org.jetbrains.research.ml.ast.transformations.commands

import com.intellij.application.options.CodeStyle
import com.intellij.formatting.FormattingContext
import com.intellij.formatting.FormattingModel
import com.intellij.lang.LanguageFormatting
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParserFacade
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyElementGenerator
import java.util.concurrent.Callable






/**
 * Represents a range of adjacent siblings from [startPsi] (inclusive) to [endPsi] (inclusive),
 * siblings are the first (prev, next) siblings that meet a [condition]
 */
class RestorablePsiElement(private var psiElement: PsiElement) {

    enum class Type {
        ONLY_CHILD, LEFT_CHILD, RIGHT_CHILD, MIDDLE_CHILD
    }

    private var psiText: String = psiElement.text
    private val project = psiElement.project

    private var prevSibling: PsiElement? = findNextSiblingOnCondition(
        { it.prevSibling },
        { this.prevSibling = it.newPsi }
    )
    private var nextSibling: PsiElement? = findNextSiblingOnCondition(
        { it.nextSibling },
        { this.nextSibling = it.newPsi }
    )
    private var parent = findParent()
    private val type = findType()
    private val indents = RestorableIndents()

    private fun isIndent(psiElement: PsiElement) = psiElement is PsiWhiteSpace

    inner class RestorableIndents {
        private val project = psiElement.project
        private val prevIndent = findIndentBetween(prevSibling, psiElement)
        private val nextIndent = findIndentBetween(psiElement, nextSibling)

        private val formattingModel = createFormattingModel()

        private fun createFormattingModel(): FormattingModel {
            val builder = LanguageFormatting.INSTANCE.forContext(psiElement)
            require(builder != null) { "LanguageFormatting is null for ${psiElement.text}" }
            val settings: CodeStyleSettings = CodeStyle.getSettings(project)
            return builder.createModel(FormattingContext.create(psiElement, settings))
        }

        private fun generateIndentFromText(text: String): PsiElement {
            return PsiParserFacade.SERVICE.getInstance(project).createWhiteSpaceFromText(text)
        }

        private fun PsiElement.replaceIndent(newIndentText: String) {
            formattingModel.replaceWhiteSpace(this.textRange, newIndentText)
            formattingModel.commitChanges()
        }

        private fun findIndentBetween(leftPsi: PsiElement?, rightPsi: PsiElement?): PsiElement? {
            fun findIndent(firstPsi: PsiElement, secondPsi: PsiElement?, getNextPsi: (PsiElement) -> PsiElement?): PsiElement? {
                val nextPsi = getNextPsi(firstPsi)
                return nextPsi?.let {
                    if (isIndent(it)) {
                        require(getNextPsi(it) === secondPsi) { "There is more than one indent between left and right psi" }
                        it
                    } else {
                        require(it === secondPsi) { "There is not an indent between left and right psi" }
                        null
                    }
                }
            }

            return if (leftPsi != null) {
                findIndent(leftPsi, rightPsi) { it.nextSibling }
            } else if (rightPsi != null) {
                findIndent(rightPsi, leftPsi) { it.prevSibling }
            } else {
                error("Cannot get indent between two nulls")
            }
        }

        fun addElement(psiElementToAdd: PsiElement, add: (PsiElement) -> PsiElement): PsiElement {
            val addedPsiElement = WriteCommandAction.runWriteCommandAction<PsiElement>(project) {
                add(psiElementToAdd)
            }
            val newPrevIndent = findIndentBetween(prevSibling, addedPsiElement)
            val newNextIndent = findIndentBetween(addedPsiElement, nextSibling)
            checkNewIndent(newPrevIndent, prevIndent) { parent.addBefore(it, addedPsiElement) }
            checkNewIndent(newNextIndent, nextIndent) { parent.addAfter(it, addedPsiElement) }
            return addedPsiElement
        }

        private fun checkNewIndent(newIndent: PsiElement?, oldIndent: PsiElement?, addIndent: (PsiElement) -> Unit) {
            if (oldIndent == null && newIndent == null)
                return
            if (newIndent == null) {
                val indentToAdd = generateIndentFromText(oldIndent!!.text)
                WriteCommandAction.runWriteCommandAction(project) {
                    addIndent(indentToAdd)
                }
                return
            }
            if (oldIndent == null) {
                error("New indent after deletion")
            }
            if (oldIndent.text != newIndent.text) {
                WriteCommandAction.runWriteCommandAction(project) {
                    newIndent.replaceIndent(oldIndent.text)
                }
            }
        }
    }



    private fun findType(): Type {
        if (prevSibling == null && nextSibling == null) {
            return Type.ONLY_CHILD
        }
        if (prevSibling == null) {
            return Type.LEFT_CHILD
        }
        if (nextSibling == null) {
            return Type.RIGHT_CHILD
        }
        return Type.MIDDLE_CHILD
    }

    private fun findNextSiblingOnCondition(
        getSibling: (PsiElement) -> PsiElement?,
        onUpdate: ((PsiUpdatesPublisher.UpdatedPsi) -> Unit)? = null,
        initPsi: PsiElement = psiElement
    ): PsiElement? {
        var nextSibling = getSibling(initPsi)
        if (nextSibling != null && isIndent(nextSibling)) {
            nextSibling = getSibling(nextSibling)
        }
        require(nextSibling == null || !isIndent(nextSibling)) { "More than one indents between psiElement and its sibling" }
        return nextSibling?.also { s -> onUpdate?.let { PsiUpdatesPublisher.subscribe(s, onUpdate) } }
    }

    private fun findParent(): PsiElement {
        return psiElement.parent!!.also { PsiUpdatesPublisher.subscribe(it) { this.parent = it.newPsi } }
    }


    fun delete() {
        WriteCommandAction.runWriteCommandAction(project) {
            psiElement.delete()
        }
    }

    private fun generateFromText(): PsiElement {
        val generator = PyElementGenerator.getInstance(project)
        return generator.createFromText(LanguageLevel.getDefault(), PsiElement::class.java, psiText)
    }


    fun restore() {
        val psiElementToAdd = generateFromText()
        val addedPsiElement = when (type) {
            Type.ONLY_CHILD -> indents.addElement(psiElementToAdd) { parent.add(it) }
            Type.LEFT_CHILD -> indents.addElement(psiElementToAdd) { parent.addBefore(it, nextSibling) }
            Type.RIGHT_CHILD -> indents.addElement(psiElementToAdd) { parent.addAfter(it, prevSibling) }
            Type.MIDDLE_CHILD -> indents.addElement(psiElementToAdd) { parent.addBefore(it, nextSibling) }
        }
        PsiUpdatesPublisher.notify(PsiUpdatesPublisher.UpdatedPsi(psiElement, addedPsiElement))
    }
}



// Note, you cannot redo it one more time after undo
class DeleteCommand(private val psiElement: PsiElement) : CommandProvider<Unit>() {
    //  Creates restorablePsiElement just before psiElement deletion
    lateinit var restorablePsiElement: RestorablePsiElement

    override fun redo(): Callable<Unit> {
        restorablePsiElement = RestorablePsiElement(psiElement)
        return Callable { restorablePsiElement.delete() }
    }

    override fun undo(): Callable<*> {
        return Callable { restorablePsiElement.restore() }
    }
}

//class DelayedDeleteCommand(private val psiElement: PsiElement) : CommandProvider<PsiElement, Unit>() {
////  Creates restorablePsiElement just before psiElement deletion
//    lateinit var restorablePsiElement: RestorablePsiElement
//
//    override fun redo(input: PsiElement): Callable<Unit> {
//        restorablePsiElement = RestorablePsiElement(psiElement)
//        return Callable { restorablePsiElement.delete() }
//    }
//
//    override fun undo(input: PsiElement): Callable<*> {
//        return Callable { restorablePsiElement.restore() }
//    }
//}


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


