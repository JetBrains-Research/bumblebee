package org.jetbrains.research.ml.ast.util

import com.intellij.codeInsight.daemon.impl.DefaultHighlightVisitorBasedInspection
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SyntaxTraverser
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.impl.references.PyOperatorReference

fun PsiElement.isCorrect(): Boolean {
    if (this.hasSyntaxError() || !this.canBeResolved()) {
        return false
    }
    return (this as PsiFile).countHighlightErrors() == 0
}

// Does not help find errors like unresolved reference and etc
fun PsiElement.hasSyntaxError(): Boolean = this.countPsiErrorElements() != 0

private fun PsiElement.countPsiErrorElements(): Int {
    return ApplicationManager.getApplication().runReadAction<Int> {
        val elements = PsiTreeUtil.collectElements(this) { it is PsiErrorElement }
        elements.size
    }
}

// Find unresolved references errors
fun PsiElement.canBeResolved(): Boolean {
    return ApplicationManager.getApplication().runReadAction<Boolean> {
        SyntaxTraverser.psiTraverser()
            .withRoot(this)
            .filter { it.reference != null }
            .all { it.canGoToDeclaration() }
    }
}

private fun PsiElement.canGoToDeclaration(): Boolean {
    if (this.reference?.resolve() != null) {
        return true
    }
    // If we have the following code:
    //  s = input()
    //  a = s[1]
    // <this.reference?.resolve() == null> because the string type was not inferred
    // But we can go to the <s> part in the <s[1]> and try to resolve this element
    val operatorReference = this.reference as? PyOperatorReference
    if (operatorReference != null && operatorReference.receiver?.reference?.resolve() != null) {
        return true
    }
    return false
}

fun PsiFile.countHighlightErrors(): Int {
    return ApplicationManager.getApplication().runReadAction<Int> {
        val highlightings = DefaultHighlightVisitorBasedInspection.runGeneralHighlighting(this, false, true)
        val errors = highlightings.filter { it.second.severity.name == "ERROR" }
        errors.size
    }
}
