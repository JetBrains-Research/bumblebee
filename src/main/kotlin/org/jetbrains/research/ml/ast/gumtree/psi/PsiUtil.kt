package org.jetbrains.research.ml.ast.gumtree.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.impl.*
import com.jetbrains.python.psi.impl.stubs.PyDecoratorCallElementType

val PsiElement.isLeaf: Boolean
    get() = this.children.isEmpty()

// TODO: it does not support async and yield keywords
val PsiElement.intermediateElementLabel: String
    get() = ApplicationManager.getApplication().runReadAction<String> {
        when (this) {
            is PyBinaryExpressionImpl -> getSymbolByStringRepresentation(operator.toString())
            // Expression like -1 or not a
            is PyPrefixExpressionImpl -> getSymbolByStringRepresentation(operator.toString())
            // Expression like +=, -= and so on, for example a += 5
            is PyAugAssignmentStatementImpl -> getSymbolByStringRepresentation(operation?.text ?: "")
            is PyFormattedStringElementImpl -> content
            is PyImportElementImpl, is PyDecoratorImpl -> ""
            is PyBaseElementImpl<*> -> name ?: ""
            else -> ""
        }
    }

private fun getSymbolByStringRepresentation(stringRepr: String): String {
    return when (stringRepr) {
        PyTokenTypes.PLUS.toString() -> "+"
        PyTokenTypes.MINUS.toString() -> "-"
        PyTokenTypes.MULT.toString() -> "*"
        PyTokenTypes.EXP.toString() -> "**"
        PyTokenTypes.DIV.toString() -> "/"
        PyTokenTypes.FLOORDIV.toString() -> "//"
        PyTokenTypes.PERC.toString() -> "%"
        PyTokenTypes.LTLT.toString() -> "<<"
        PyTokenTypes.GTGT.toString() -> ">>"
        PyTokenTypes.TILDE.toString() -> "~"
        PyTokenTypes.LT.toString() -> "<"
        PyTokenTypes.GT.toString() -> ">"
        PyTokenTypes.LE.toString() -> "<="
        PyTokenTypes.GE.toString() -> ">="
        PyTokenTypes.EQEQ.toString() -> "=="
        PyTokenTypes.EQ.toString() -> "="
        PyTokenTypes.NOT_KEYWORD.toString() -> "not"
        PyTokenTypes.AND_KEYWORD.toString() -> "and"
        PyTokenTypes.OR_KEYWORD.toString() -> "or"
        else -> stringRepr
    }
}

/*
* See the [docs/PsiTreeConverter.md] document for more details
* */
val PsiElement.label: String
    get() = ApplicationManager.getApplication().runReadAction<String> {
        if (this.isLeaf) {
            this.text
        } else {
            this.intermediateElementLabel
        }
    }
