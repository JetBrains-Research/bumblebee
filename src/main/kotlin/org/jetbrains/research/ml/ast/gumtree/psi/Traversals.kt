package org.jetbrains.research.ml.ast.gumtree.psi

import com.intellij.psi.PsiElement
import org.jetbrains.research.ml.ast.gumtree.tree.PsiTreeConverter.filterWhiteSpaces
import org.jetbrains.research.ml.ast.gumtree.tree.PsiTreeConverter.isWhiteSpace
import java.util.*

fun PsiElement.preOrder(): Iterable<PsiElement> {
    return object : Iterable<PsiElement> {
        override fun iterator(): Iterator<PsiElement> {
            return object : Iterator<PsiElement> {
                val currentNodes: Stack<PsiElement> = Stack()

                init {
                    if (!this@preOrder.isWhiteSpace) {
                        currentNodes.add(this@preOrder)
                    }
                }

                override fun hasNext(): Boolean {
                    return currentNodes.size != 0
                }

                override fun next(): PsiElement {
                    val c = currentNodes.pop()
                    currentNodes.addAll(c.children.filterWhiteSpaces().reversed())
                    return c
                }
            }
        }
    }
}

fun PsiElement.postOrder(): Iterable<PsiElement> {
    return object : Iterable<PsiElement> {
        override fun iterator(): Iterator<PsiElement> {
            return object : Iterator<PsiElement> {
                val currentNodes: Stack<PsiElement> = Stack()

                init {
                    if (!this@postOrder.isWhiteSpace) {
                        currentNodes.add(this@postOrder)
                    }
                    addAllUntilLeftLeaf()
                }

                override fun hasNext(): Boolean {
                    return currentNodes.size != 0
                }

                override fun next(): PsiElement {
                    val c = currentNodes.pop()
                    if (hasNext() && currentNodes.peek().children.filterWhiteSpaces().lastOrNull() != c) {
                        addAllUntilLeftLeaf()
                    }
                    return c
                }

                private fun addAllUntilLeftLeaf() {
                    var peek = currentNodes.peek()
                    while (peek.children.isNotEmpty()) {
                        currentNodes.addAll(peek.children.filterWhiteSpaces().reversed())
                        peek = currentNodes.peek()
                    }
                }
            }
        }
    }
}
