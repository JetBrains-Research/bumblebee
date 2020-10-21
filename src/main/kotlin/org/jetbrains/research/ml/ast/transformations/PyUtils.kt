package org.jetbrains.research.ml.ast.transformations

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyElementGenerator

object PyUtils {
    fun commentElement(generator: PyElementGenerator, element: PsiElement): PsiElement {
        return generator.createFromText(LanguageLevel.PYTHON36, PsiComment::class.java, "# ${element.text}")
    }
}
