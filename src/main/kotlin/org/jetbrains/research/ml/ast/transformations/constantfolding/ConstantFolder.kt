package org.jetbrains.research.ml.ast.transformations.constantfolding

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyExpression
import org.jetbrains.research.ml.ast.transformations.createBoolLiteralExpression
import org.jetbrains.research.ml.ast.transformations.createExpressionFromNumber
import kotlin.test.fail

class ConstantFolder(private val generator: PyElementGenerator) {
    private val evaluator = PyEvaluatorImproved()

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
            is Boolean -> { -> expression.replace(generator.createBoolLiteralExpression(result)) }
            is Number -> { -> expression.replace(generator.createExpressionFromNumber(result)) }
            is String -> { -> expression.replace(generator.createStringLiteralFromString(result)) }
            is List<*> -> run {
                val simplifyElements = result.map {
                    simplifyAllSubexpressionsDelayed(
                        it as? PyExpression
                            ?: fail("Evaluated list elements should be PyExpressions")
                    )
                }
                return@run {
                    val newList = expression.replace(generator.createListLiteral()) as PyExpression
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
            is PyExpression -> run {
                val simplifyNewExpression = simplifyAllSubexpressionsDelayed(result)
                return@run { expression.replace(simplifyNewExpression()) }
            }
            else -> null
        }
}
