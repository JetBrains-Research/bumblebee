package org.jetbrains.research.ml.ast.transformations

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyAssignmentStatement
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyExpression

object PyUtils {
    fun braceExpression(expression: PyExpression): PyExpression {
        val generator = PyElementGenerator.getInstance(expression.project)
        return generator.createExpressionFromText(LanguageLevel.PYTHON36, "(${expression.text})")
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
