package org.jetbrains.research.ml.ast.gumtree.tree

import com.github.gumtreediff.tree.ITree
import com.github.gumtreediff.tree.TreeContext
import com.intellij.psi.PsiElement
import org.jetbrains.research.ml.ast.gumtree.psi.label
import java.util.*

object PsiTreeConverter {
    private const val WHITE_SPACE_TYPE = "WHITE_SPACE"

    /**
     * Convert PSI to GumTree, storing already converted GumTree parent nodes and corresponding PSI child nodes:
     *       PSI:              GumTree:
     *  | children of A |       | A |
     *  | children of B |       | B |
     *  | children of C |       | C |
     *        ....               ...
     *  | children of Z |       | Z |
     *
     *  On each iteration children from PSI are converted to GumTree format and added to GumTree parents one by one;
     *  their PSI children are added to the corresponding place in the PSI children collection.
     *
     *  Ignore nodes with WHITE_SPACE_TYPE
     */
    fun convertTree(psiRoot: PsiElement, numbering: Numbering): TreeContext {
        val context = TreeContext()
        context.root = context.createTree(psiRoot)
        val gumTreeParents: Queue<ITree> = LinkedList(listOf(context.root))
        val psiChildren: Queue<List<PsiElement>> = LinkedList(listOf(psiRoot.children.filterWhiteSpaces().toList()))

        while (psiChildren.isNotEmpty()) {
            val parent = gumTreeParents.poll()
            psiChildren.poll().forEach {
                val tree = context.createTree(it)
                tree.setParentAndUpdateChildren(parent)
                gumTreeParents.add(tree)
                psiChildren.add(it.children.filterWhiteSpaces().toList())
            }
        }

        numbering.number(psiRoot, context)
        return context
    }

    val PsiElement.isWhiteSpace: Boolean
        get() = this.node.elementType.toString() == WHITE_SPACE_TYPE

    fun Array<PsiElement>.filterWhiteSpaces(): List<PsiElement> = this.filter { !it.isWhiteSpace }

    // Create GumTree tree
    private fun TreeContext.createTree(psiTree: PsiElement): ITree {
        val typeLabel = psiTree.node.elementType.toString()
        return this.createTree(typeLabel.hashCode(), psiTree.label, typeLabel)
    }
}
