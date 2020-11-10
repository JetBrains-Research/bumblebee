package org.jetbrains.research.ml.ast.transformations

import com.intellij.psi.PsiElement

object PsiTreeUtils {
    fun findChildrenByPredicate(element: PsiElement, predicate: (PsiElement) -> Boolean): Boolean {
        var cur = element.firstChild
        while (cur != null) {
            if (predicate(cur)) {
                return true
            }
            cur = cur.nextSibling
        }
        return false
    }
}
