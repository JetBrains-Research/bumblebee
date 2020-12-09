package org.jetbrains.research.ml.ast.transformations.constantfolding

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyFile
import org.jetbrains.research.ml.ast.transformations.createBoolLiteralExpression
import org.jetbrains.research.ml.ast.transformations.createExpressionFromNumber

class ConstantFolder(private val generator: PyElementGenerator, file: PyFile) {
    private val evaluator = PyEvaluatorImproved(file)

    fun simplifyAllSubexpressionsDelayed(element: PsiElement): () -> PsiElement {
        if (element is PyExpression) {
            simplifyByEvaluation(element)?.let { return it }
        }

        // Otherwise, simply fold all constants in subtrees
        val simplifyChildren = element.children.map { simplifyAllSubexpressionsDelayed(it) }
        return {
            simplifyChildren.forEach { it() }
            element
        }
    }

    private fun simplifyByEvaluation(expression: PyExpression): (() -> PsiElement)? =
        when (val result = evaluator.evaluate(expression)) {
            is PyEvaluatorImproved.PyBool ->
                { -> expression.replace(generator.createBoolLiteralExpression(result.value)) }
            is PyEvaluatorImproved.PyInt ->
                { -> expression.replace(generator.createExpressionFromNumber(result.value)) }
            is PyEvaluatorImproved.PyString ->
                { -> expression.replace(generator.createStringLiteralFromString(result.string)) }
            is PyEvaluatorImproved.PySequence -> run {
                val emptyLiteral = when (result.kind ?: return@run null) {
                    PyEvaluatorImproved.PySequence.PySequenceKind.LIST -> generator.createListLiteral()
                    PyEvaluatorImproved.PySequence.PySequenceKind.TUPLE ->
                        generator.createExpressionFromText(LanguageLevel.PYTHON39, "()")
                }
                val simplifyElements = result.elements.map { simplifyAllSubexpressionsDelayed(it) }
                return@run {
                    val newList = expression.replace(emptyLiteral) as PyExpression
                    var anchor: PyExpression? = null
                    for (simplifyElement in simplifyElements) {
                        anchor = generator.insertItemIntoListRemoveRedundantCommas(
                            newList,
                            anchor,
                            simplifyElement() as PyExpression
                        ) as PyExpression
                    }
                    newList
                }
            }
            is PyEvaluatorImproved.PyExpressionResult -> run {
                val simplifyNewExpression = simplifyAllSubexpressionsDelayed(result.expression)
                return@run { expression.replace(simplifyNewExpression()) }
            }
            else -> null
        }
}
