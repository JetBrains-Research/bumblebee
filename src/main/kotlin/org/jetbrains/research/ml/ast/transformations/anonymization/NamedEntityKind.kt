package org.jetbrains.research.ml.ast.transformations.anonymization

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyImportElement
import com.jetbrains.python.psi.PyNamedParameter
import com.jetbrains.python.psi.PyTargetExpression

enum class NamedEntityKind(val prefix: String) {
    Function("f"),
    Variable("v"),
    Class("c"),
    Parameter("p"),
    NamedImport("m"),
    Lambda("l");
    companion object {
        fun getElementKind(element: PsiElement): NamedEntityKind? = when (element) {
            is PyFunction -> Function
            is PyClass -> Class
            is PyNamedParameter -> Parameter
            is PyTargetExpression ->
                if (element.parent is PyImportElement) {
                    NamedImport
                } else {
                    Variable
                }
            else -> null
        }
    }
}

class NamedEntityKindCounter {
    fun next(kind: NamedEntityKind): Int {
        val nextValue = counter.getOrDefault(kind, 0) + 1
        counter[kind] = nextValue
        return nextValue
    }

    private val counter: MutableMap<NamedEntityKind, Int> = mutableMapOf()
}
