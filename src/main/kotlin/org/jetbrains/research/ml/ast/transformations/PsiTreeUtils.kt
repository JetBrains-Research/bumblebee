package org.jetbrains.research.ml.ast.transformations

import com.intellij.psi.PsiElement

object PsiTreeUtils {
    fun findFirstChildrenOrNull(element: PsiElement, predicate: (PsiElement) -> Boolean): PsiElement? {
        var cur = element.firstChild
        while (cur != null) {
            if (predicate(cur)) {
                return cur
            }
            cur = cur.nextSibling
        }
        return null
    }
}
