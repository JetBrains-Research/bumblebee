package org.jetbrains.research.ml.ast.gumtree.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.impl.*

val PsiElement.isLeaf: Boolean
    get() = this.children.isEmpty()

// TODO: we don't store "in" keyword for loops in labels. Is it ok?
// TODO: it does not support async keyword now and type annotations
val PsiElement.intermediateElementLabel: String
    get() = ApplicationManager.getApplication().runReadAction<String> {
        when (this) {
            // TODO: is it ok that I use operator?.specialMethodName (__add__ for example) for PyBinaryExpressionImpl
            //  and operator.toString() (PY:PLUS for example) for PyPrefixExpressionImpl??
            is PyBinaryExpressionImpl -> operator?.specialMethodName ?: operator.toString()
            // Expression like -1 or not a
            is PyPrefixExpressionImpl -> operator.toString()
            // Expression like +=, -= and so on, for example a += 5
            // TODO: is it ok to get text?
            is PyAugAssignmentStatementImpl -> operation?.text ?: ""
            // TODO: is it ok to store content: f"text {1}"
            is PyFormattedStringElementImpl -> content
            is PyImportElementImpl -> ""
            // We should separate the cases <yield from [1, 2, 3]> and <yield 3> in the GumTree tree
            is PyYieldExpressionImpl -> if ("from" in text) "from" else ""
            is PyBaseElementImpl<*> -> name ?: ""
            else -> ""
        }
    }

/*
* See the [docs/PsiTreeConverter.md] document for more details
* */
val PsiElement.label: String
    get() = ApplicationManager.getApplication().runReadAction<String> {
        val label = if (this.isLeaf) {
            this.text
        } else {
            this.intermediateElementLabel
        }
        label
    }
