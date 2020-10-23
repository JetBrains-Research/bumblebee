package org.jetbrains.research.ml.ast.transformations

import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyIfPart
import com.jetbrains.python.psi.PyIfStatement

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
}
