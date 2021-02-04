package org.jetbrains.research.ml.ast.gumtree.diff

import com.github.gumtreediff.actions.model.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.IncorrectOperationException
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyElementGenerator
import org.jetbrains.research.ml.ast.gumtree.tree.Numbering
import org.jetbrains.research.ml.ast.gumtree.tree.Numbering.PsiTreeUtils.Companion.id
import org.jetbrains.research.ml.ast.gumtree.tree.Numbering.PsiTreeUtils.Companion.setId

data class PsiTransformation(
    private val srcPsi: PsiElement,
    private val dstPsi: PsiElement,
    private val numbering: Numbering
) {
    val srcPsiNodes: MutableList<PsiElement> = getPsiNodes(srcPsi)
    var dstPsiNodes: MutableList<PsiElement> = getPsiNodes(dstPsi)
    val insertedNodesIds: MutableSet<Int> = HashSet()

    private fun getPsiNodes(psi: PsiElement): MutableList<PsiElement> {
        return ApplicationManager.getApplication().runReadAction<MutableList<PsiElement>> {
            numbering.iterable(psi).toMutableList()
        }
    }
}

class PsiElementTransformer(
    project: Project
) {
    private val generator = PyElementGenerator.getInstance(project)

    // Should be run in WriteAction
    private fun applyAction(action: Action, transformation: PsiTransformation) {
        // TODO: should we store actions to compare with transformations actions??
        when (action) {
            is Delete -> action.applyDelete(transformation)
            is Insert -> action.apply(transformation)
            is Update -> action.apply(transformation)
            is Move -> action.apply(transformation)
            else -> error("Unknown GumTree action ${action.name}")
        }
    }

    // TODO: it seems we should use simple <applyAction>
    fun applyActions(actions: List<Action>, transformation: PsiTransformation) {
        actions.forEach {
            try {
                applyAction(it, transformation)
            } catch (e: IncorrectOperationException) {
                println("Try execute action ${it.name} with node: id=${it.node.id}, label=${it.node.label}, but fail")
            }
        }
    }

    private fun Action.getPsiElementById(psiElements: List<PsiElement>, errorMessagePrefix: String = ""): PsiElement {
        val id = this.node.id
        require(id < psiElements.size) { "${errorMessagePrefix}PSI tree does not contain node with id $id" }
        return psiElements[id]
    }

    private fun executeInsert(
        element: PsiElement,
        parent: PsiElement,
        position: Int,
        transformation: PsiTransformation
    ) {
        if (!element.isValid || !parent.isValid) {
            return
        }
        val child = if (position < parent.children.size) {
            parent.children[position]
        } else {
            parent.lastChild
        }
        parent.addAfter(element, child)
        transformation.insertedNodesIds.add(element.id!!)
    }

    private fun Action.applyDelete(transformation: PsiTransformation) {
        val psiElement = this.getPsiElementById(transformation.srcPsiNodes, "Source ")
        psiElement.safeDelete()
    }

    // [psiNodes] - list of psiElements (srcPsiNodes or dstPsiNodes) in which to search for the [node] from the action
    private fun Action.applyInsert(
        psiNodes: List<PsiElement>,
        parentId: Int,
        position: Int,
        errorMessagePrefix: String = "",
        transformation: PsiTransformation
    ) {
        val parentNode =
            if ((parentId in transformation.insertedNodesIds && parentId < transformation.dstPsiNodes.size) ||
                parentId >= transformation.srcPsiNodes.size
            ) {
                transformation.dstPsiNodes[parentId]
            } else {
                transformation.srcPsiNodes[parentId]
            }
        val psiElement = this.getPsiElementById(psiNodes, errorMessagePrefix)
        executeInsert(psiElement, parentNode, position, transformation)
    }

    private fun Insert.apply(transformation: PsiTransformation) {
        this.applyInsert(transformation.dstPsiNodes, this.parent.id, this.position, "Destination ", transformation)
    }

    private fun Move.apply(transformation: PsiTransformation) {
        this.applyInsert(transformation.srcPsiNodes, this.parent.id, this.position, "Source ", transformation)
        this.applyDelete(transformation)
    }

    private fun Update.apply(transformation: PsiTransformation) {
        val psiElement = this.getPsiElementById(transformation.srcPsiNodes, "Source ")
        val newText = psiElement.text.replace(this.node.label, this.value)
        val newElement = generator.createFromText(LanguageLevel.getDefault(), PsiElement::class.java, newText)
        if (newElement.isValid) {
            newElement.setId(psiElement.id)
            transformation.srcPsiNodes[this.node.id] = psiElement.replace(newElement)
        }
    }

    private fun PsiElement.safeDelete() {
        if (this.isValid) {
            this.delete()
        }
    }
}
