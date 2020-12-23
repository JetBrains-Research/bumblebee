package org.jetbrains.research.ml.ast.transformations

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyParenthesizedExpression

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

    fun getPyElementThroughParens(element: PsiElement): PsiElement? {
        var result: PsiElement? = element
        while (result is PyParenthesizedExpression) {
            result = result.containedExpression
        }
        return result
    }
}
