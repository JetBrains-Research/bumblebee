package org.jetbrains.research.ml.ast.transformations

import com.intellij.psi.PsiElement

object PyUtils {
    fun createAssignment(target: PsiElement, value: PsiElement): PyAssignmentStatement {
        val generator = PyElementGenerator.getInstance(target.project)
        return generator.createFromText(
            LanguageLevel.PYTHON36,
            PyAssignmentStatement::class.java,
            "${target.text} = ${value.text}"
        )
    }
}
