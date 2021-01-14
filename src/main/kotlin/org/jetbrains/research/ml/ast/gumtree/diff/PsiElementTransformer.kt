package org.jetbrains.research.ml.ast.gumtree.diff

import com.github.gumtreediff.actions.model.*
import com.github.gumtreediff.tree.ITree
import com.intellij.codeInsight.editorActions.smartEnter.SmartEnterProcessor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.util.IncorrectOperationException
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyElementGenerator
import org.jetbrains.research.ml.ast.gumtree.psi.label
import org.jetbrains.research.ml.ast.gumtree.tree.Numbering
import org.jetbrains.research.ml.ast.gumtree.tree.Numbering.PsiTreeUtils.Companion.id
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class PsiElementTransformer(
    project: Project,
    val srcPsi: PsiElement,
    dstPsi: PsiElement,
    private val numbering: Numbering
) {
    private val generator = PyElementGenerator.getInstance(project)

    private val srcPsiNodes = getPsiNodes(srcPsi)
    private val dstPsiNodes = getPsiNodes(dstPsi)
//
//    private lateinit var srcEditor: Editor
//    private lateinit var srcPsiFile: VirtualFile
//
//    init {
//        ApplicationManager.getApplication().invokeAndWait {
//            srcEditor = getEditor(srcPsi)
//        }
//    }

    private fun getPsiNodes(psi: PsiElement): List<PsiElement> {
        return ApplicationManager.getApplication().runReadAction<List<PsiElement>> {
            numbering.iterable(psi).toList()
        }
    }

    // Should be run in WriteAction
    private fun applyAction(action: Action, insertedNode: MutableSet<Int>) {
        // TODO: should we store actions to compare with transformations actions??
        when (action) {
            is Delete -> action.applyDelete()
            is Insert -> action.apply(insertedNode)
            is Update -> action.apply()
            is Move -> action.apply(insertedNode)
            else -> error("Unknown GumTree action ${action.name}")
        }
    }

//    private fun getEditor(psiElement: PsiElement): Editor {
//        srcPsiFile = psiElement.containingFile.virtualFile
//        return ApplicationManager.getApplication().runReadAction<Editor> {
//            val doc = FileDocumentManager.getInstance().getDocument(srcPsiFile)!!
//            EditorFactory.getInstance().createEditor(doc, project)!!
//        }
//    }

    fun applyActions(actions: List<Action>) {
        println("_____")
        println(srcPsi.text)
        println("_____")
        val insertedNode: MutableSet<Int> = HashSet()
        val actionsQueue: Queue<Action> = LinkedList(actions)
        while (actionsQueue.isNotEmpty()) {
            val action = actionsQueue.poll()
//            applyAction(action)
            try {
                println("_____")
                applyAction(action, insertedNode)
                println(srcPsi.text)
                println("_____")
            } catch (e: IncorrectOperationException) {
                println("Try execute action $action, but fail")
                actionsQueue.add(action)
            }
        }
//        EditorFactory.getInstance().releaseEditor(srcEditor)
//        SmartEnterProcessor.commitDocument(srcEditor)
//        PsiManager.getInstance(project).findFile(srcPsiFile)!!
    }

    private fun Action.getPsiElementById(psiElements: List<PsiElement>, errorMessagePrefix: String = ""): PsiElement {
        val id = this.node.id
        require(id < psiElements.size) { "${errorMessagePrefix}PSI tree does not contain node with id $id" }
        return psiElements[id]
    }

    private fun executeInsert(element: PsiElement, parent: PsiElement, position: Int,
                              gumTreeNode: ITree, insertedNode: MutableSet<Int>) {
        if (!element.isValid) {
            return
        }
        println("$position")
        val child = if (position < parent.children.size) {
            parent.children[position]
        } else {
            parent.lastChild
        }
        // We can invalidate PSI by applying GumTree transformations.
        // GumTree has <delete> actions for incorrect <insert> action, but later
        try {
            println(element.text)
            println(parent.text)
            parent.addAfter(element, child)
            insertedNode.add(gumTreeNode.id)
            // Filter according to postOder traversal
            insertedNode.addAll(element.children.mapNotNull{ it.id }.filter { it >= gumTreeNode.id })
        } catch (e: IncorrectOperationException) {
            println("Try execute insert actions, but fail")
        }
        println(insertedNode)
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
        errorMessagePrefix: String = "",
        insertedNode: MutableSet<Int>
    ) {
        val psiElement = this.getPsiElementById(psiNodes, errorMessagePrefix)
        // Don't insert a node which does exist
        if (parentId in insertedNode) {
            return
        }
        val parentNode = if (parentId > srcPsiNodes.size) {
            dstPsiNodes[parentId]
        } else {
            srcPsiNodes[parentId]
        }
        println("ids ${node.id} $parentId")
        println("labels ±${node.label}± ±${psiElement.label}± ±${parentNode.text}±")
//        val parentNode = srcPsiNodes[parentId]
        executeInsert(psiElement, parentNode, position, this.node, insertedNode)
    }

    private fun Insert.apply(insertedNode: MutableSet<Int>) = this.applyInsert(dstPsiNodes, this.parent.id, this.position, "Destination ", insertedNode)

    private fun Move.apply(insertedNode: MutableSet<Int>) {
        this.applyInsert(srcPsiNodes, this.parent.id, this.position, "Source ", insertedNode)
        this.applyDelete()
    }

    private fun Update.apply() {
        val psiElement = this.getPsiElementById(srcPsiNodes, "Source ")
        val newText = psiElement.text.replace(this.node.label, this.value)
        val newElement = generator.createFromText(LanguageLevel.getDefault(), PsiElement::class.java, newText)
        psiElement.replace(newElement)
    }
}
