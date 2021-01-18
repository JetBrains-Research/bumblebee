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

class PsiElementTransformer(
    project: Project,
    srcPsi: PsiElement,
    dstPsi: PsiElement,
    private val numbering: Numbering
) {
    private val generator = PyElementGenerator.getInstance(project)

    private val srcPsiNodes = getPsiNodes(srcPsi)
    private val dstPsiNodes = getPsiNodes(dstPsi)

    private fun getPsiNodes(psi: PsiElement): MutableList<PsiElement> {
        return ApplicationManager.getApplication().runReadAction<MutableList<PsiElement>> {
            numbering.iterable(psi).toMutableList()
        }
    }

    private data class NodesObserver(
        val insertedNodesIds: MutableSet<Int> = HashSet()
    )

    // Should be run in WriteAction
    private fun applyAction(action: Action, nodesObserver: NodesObserver) {
        // TODO: should we store actions to compare with transformations actions??
        when (action) {
            is Delete -> action.applyDelete()
            is Insert -> action.apply(nodesObserver)
            is Update -> action.apply()
            is Move -> action.apply(nodesObserver)
            else -> error("Unknown GumTree action ${action.name}")
        }
    }

    // TODO: it seems we should use simple <applyAction>
    fun applyActions(actions: List<Action>) {
        val nodesObserver = NodesObserver()
        actions.forEach {
            try {
                applyAction(it, nodesObserver)
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

    private fun executeInsert(element: PsiElement, parent: PsiElement, position: Int, nodesObserver: NodesObserver) {
        if (!element.isValid || !parent.isValid) {
            return
        }
        val child = if (position < parent.children.size) {
            parent.children[position]
        } else {
            parent.lastChild
        }
        parent.addAfter(element, child)
        nodesObserver.insertedNodesIds.add(element.id!!)
    }

    private fun Action.applyDelete() {
        val psiElement = this.getPsiElementById(srcPsiNodes, "Source ")
        psiElement.safeDelete()
    }

    // [psiNodes] - list of psiElements (srcPsiNodes or dstPsiNodes) in which to search for the [node] from the action
    private fun Action.applyInsert(
        psiNodes: List<PsiElement>,
        parentId: Int,
        position: Int,
        errorMessagePrefix: String = "",
        nodesObserver: NodesObserver
    ) {
        val parentNode =
            if ((parentId in nodesObserver.insertedNodesIds && parentId < dstPsiNodes.size) ||
                parentId >= srcPsiNodes.size
            ) {
                dstPsiNodes[parentId]
            } else {
                srcPsiNodes[parentId]
            }
        val psiElement = this.getPsiElementById(psiNodes, errorMessagePrefix)
        executeInsert(psiElement, parentNode, position, nodesObserver)
    }

    private fun Insert.apply(nodesObserver: NodesObserver) {
        this.applyInsert(dstPsiNodes, this.parent.id, this.position, "Destination ", nodesObserver)
    }

    private fun Move.apply(nodesObserver: NodesObserver) {
        this.applyInsert(srcPsiNodes, this.parent.id, this.position, "Source ", nodesObserver)
        this.applyDelete()
    }

    private fun Update.apply() {
        val psiElement = this.getPsiElementById(srcPsiNodes, "Source ")
        val newText = psiElement.text.replace(this.node.label, this.value)
        val newElement = generator.createFromText(LanguageLevel.getDefault(), PsiElement::class.java, newText)
        if (newElement.isValid) {
            newElement.setId(psiElement.id)
            srcPsiNodes[this.node.id] = psiElement.replace(newElement)
        }
    }

    private fun PsiElement.safeDelete() {
        if (this.isValid) {
            this.delete()
        }
    }
}
