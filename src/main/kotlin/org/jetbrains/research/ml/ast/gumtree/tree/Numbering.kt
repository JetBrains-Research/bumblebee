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
        }
    }

    protected abstract fun PsiElement.iterable(): Iterable<PsiElement>
    protected abstract fun ITree.iterable(): Iterable<ITree>

    fun number(psiElement: PsiElement) {
        PsiTreeUtils.numbering(psiElement.iterable())
    }

    fun number(gumTreeContext: TreeContext) {
        gumTreeContext.root.refresh()
        TreeUtils.numbering(gumTreeContext.root.iterable())
    }

    fun number(psiElement: PsiElement, gumTreeContext: TreeContext) {
        number(psiElement)
        number(gumTreeContext)
    }
}

object PreOrderNumbering : Numbering() {
    override fun PsiElement.iterable(): Iterable<PsiElement> = this.preOrder()
    override fun ITree.iterable(): Iterable<ITree> = this.preOrder()
}

object PostOrderNumbering : Numbering() {
    override fun PsiElement.iterable(): Iterable<PsiElement> = this.postOrder()
    override fun ITree.iterable(): Iterable<ITree> = this.postOrder()
}
