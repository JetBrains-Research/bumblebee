package org.jetbrains.research.ml.ast.gumtree.tree

import com.github.gumtreediff.tree.ITree
import com.github.gumtreediff.tree.TreeContext
import com.intellij.psi.PsiElement
import org.jetbrains.research.ml.ast.gumtree.psi.getElementChildren
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
     *  If [toIgnoreWhiteSpaces] is True then ignore nodes with WHITE_SPACE_TYPE. It can be useful in several cases:
     *  - The WHITE_SPACE elements do not represent the actual difference between two PSI trees,
     *  as they are tree elements that are not related to the original Python AST.
     *  This means that for a better trees comparison in GumTree format, you can remove them from PSI.
     *  - The standard GumTree trees XML serializer uses XMLInputFactory to deserialize the tree.
     *  In general, the algorithm works as follow: an XMLEventReader is created, then you need to restore
     *  the vertex for each XMLEvent.
     *  The problem is that if the label in the XMLEvent was a newline character symbol
     *  (for example \n for Unix systems, and \r\n for Windows),
     *  then after deserialization it will be replaced by the space symbol.
     *  This is not a problem with GumTree, this is a problem with standard Java XML deserializer.
     *  In this case, it is better to remove WHITE_SPACE elements.
     */
    fun convertTree(psiRoot: PsiElement, numbering: Numbering, toIgnoreWhiteSpaces: Boolean = true): TreeContext {
        val context = TreeContext()
        context.root = context.createTree(psiRoot)
        val gumTreeParents: Queue<ITree> = LinkedList(listOf(context.root))
        val psiChildren: Queue<List<PsiElement>> = LinkedList(listOf(psiRoot.getElementChildren(toIgnoreWhiteSpaces)))

        while (psiChildren.isNotEmpty()) {
            val parent = gumTreeParents.poll()
            psiChildren.poll().forEach {
                val tree = context.createTree(it)
                tree.setParentAndUpdateChildren(parent)
                gumTreeParents.add(tree)
                psiChildren.add(it.getElementChildren(toIgnoreWhiteSpaces))
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
