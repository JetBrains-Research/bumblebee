package org.jetbrains.research.ml.ast.gumtree.tree

import com.github.gumtreediff.tree.ITree
import com.github.gumtreediff.tree.TreeContext
import com.github.gumtreediff.tree.TreeUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import org.jetbrains.research.ml.ast.gumtree.psi.postOrder
import org.jetbrains.research.ml.ast.gumtree.psi.preOrder

abstract class Numbering {

    // To be consistent with GumTree [TreeUtils]
    class PsiTreeUtils {
        companion object {
            private val psiId: Key<Int> = Key("ID")

            val PsiElement.id: Int?
                get() = ApplicationManager.getApplication().runReadAction<Int?> {
                    this.getCopyableUserData(psiId)
                }

            fun numbering(iterable: Iterable<PsiElement>) {
                iterable.forEachIndexed { i, t ->
                    t.putCopyableUserData(psiId, i)
                }
            }

            fun PsiElement.setId(id: Int?) {
                this.putCopyableUserData(psiId, id)
            }
        }
    }

    abstract fun iterable(psiElement: PsiElement): Iterable<PsiElement>
    abstract fun iterable(tree: ITree): Iterable<ITree>

    private fun number(psiElement: PsiElement) {
        PsiTreeUtils.numbering(iterable(psiElement))
    }

    private fun number(gumTreeContext: TreeContext) {
        gumTreeContext.root.refresh()
        TreeUtils.numbering(iterable(gumTreeContext.root))
    }

    fun number(psiElement: PsiElement, gumTreeContext: TreeContext) {
        number(psiElement)
        number(gumTreeContext)
    }
}

object PreOrderNumbering : Numbering() {
    override fun iterable(psiElement: PsiElement): Iterable<PsiElement> = psiElement.preOrder()
    override fun iterable(tree: ITree): Iterable<ITree> = tree.preOrder()
}

object PostOrderNumbering : Numbering() {
    override fun iterable(psiElement: PsiElement): Iterable<PsiElement> = psiElement.postOrder()
    override fun iterable(tree: ITree): Iterable<ITree> = tree.postOrder()
}
