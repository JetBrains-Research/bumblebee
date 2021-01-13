package org.jetbrains.research.ml.ast.gumtree.diff

import com.github.gumtreediff.actions.model.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.IncorrectOperationException
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyElementGenerator
import org.jetbrains.research.ml.ast.gumtree.tree.Numbering

class PsiElementTransformer(
    project: Project,
    srcPsi: PsiElement,
    dstPsi: PsiElement,
    private val numbering: Numbering
) {
    private val generator = PyElementGenerator.getInstance(project)

    private val srcPsiNodes = getPsiNodes(srcPsi)
    private val dstPsiNodes = getPsiNodes(dstPsi)

    private fun getPsiNodes(psi: PsiElement): List<PsiElement> {
        return ApplicationManager.getApplication().runReadAction<List<PsiElement>> {
            numbering.iterable(psi).toList()
        }
    }

    // Should be run in WriteAction
    fun applyAction(action: Action) {
        // TODO: should we store actions to compare with transformations actions??
        when (action) {
            is Delete -> action.applyDelete()
            is Insert -> action.apply()
            is Update -> action.apply()
            is Move -> action.apply()
            else -> error("Unknown GumTree action ${action.name}")
        }
    }

    private fun Action.getPsiElementById(psiElements: List<PsiElement>, errorMessagePrefix: String = ""): PsiElement {
        val id = this.node.id
        require(id <= psiElements.size) { "${errorMessagePrefix}PSI tree does not contain node with id $id" }
        return psiElements[id]
    }

    private fun executeInsert(element: PsiElement, parent: PsiElement, position: Int) {
        if (!element.isValid) {
            return
        }
        val child = if (position < parent.children.size) {
            parent.children[position]
        } else {
            parent.lastChild
        }
        // We can invalidate PSI by applying GumTree transformations.
        // GumTree has <delete> actions for incorrect <insert> action, but later
        try {
            parent.addAfter(element, child) as PsiElement
        } catch (e: IncorrectOperationException) {
            println("Try execute insert actions, but fail")
        }
    }

    private fun Action.applyDelete() {
        val psiElement = this.getPsiElementById(srcPsiNodes, "Source ")
        // The psiElement was not previously deleted
        if (psiElement.isValid) {
            psiElement.delete()
        }
    }

    // psiNodes - list of psiElements (srcPsiNodes or dstPsiNodes) in which to search for the [node] from the action
    private fun Action.applyInsert(
        psiNodes: List<PsiElement>,
        parentId: Int,
        position: Int,
        errorMessagePrefix: String = ""
    ) {
        val psiElement = this.getPsiElementById(psiNodes, errorMessagePrefix)
        // Don't insert a node which does exist
        if (parentId > srcPsiNodes.size) {
            return
        }
        val parentNode = srcPsiNodes[parentId]
        executeInsert(psiElement, parentNode, position)
    }

    private fun Insert.apply() = this.applyInsert(dstPsiNodes, this.parent.id, this.position, "Destination ")

    private fun Move.apply() {
        this.applyInsert(srcPsiNodes, this.parent.id, this.position, "Source ")
        this.applyDelete()
    }

    private fun Update.apply() {
        val psiElement = this.getPsiElementById(srcPsiNodes, "Source ")
        val newText = psiElement.text.replace(this.node.label, this.value)
        val newElement = generator.createFromText(LanguageLevel.getDefault(), PsiElement::class.java, newText)
        psiElement.replace(newElement)
    }
}
