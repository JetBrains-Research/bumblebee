package org.jetbrains.research.ml.ast.transformations

import com.intellij.psi.PsiElement

object PyUtils {
    fun createPyIfElsePart(ifElsePart: PyIfPart): PyIfPart {
        assert(ifElsePart.isElif)
        val generator = PyElementGenerator.getInstance(ifElsePart.project)
        val ifStatement = generator.createFromText(
            LanguageLevel.PYTHON36,
            PyIfStatement::class.java,
            "if ${ifElsePart.condition?.text ?: ""}:\n\t${ifElsePart.statementList.text}"
        )
        return ifStatement.ifPart
    }

    fun createAssignment(target: PsiElement, value: PsiElement): PyAssignmentStatement {
        val generator = PyElementGenerator.getInstance(target.project)
        return generator.createFromText(
            LanguageLevel.PYTHON36,
            PyAssignmentStatement::class.java,
            "${target.text} = ${value.text}"
        )
    }
}
