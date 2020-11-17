package org.jetbrains.research.ml.ast.transformations

import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyExpression

object PyUtils {
    fun braceExpression(expression: PyExpression): PyExpression {
        val generator = PyElementGenerator.getInstance(expression.project)
        return generator.createExpressionFromText(LanguageLevel.PYTHON36, "(${expression.text})")
    }
}
