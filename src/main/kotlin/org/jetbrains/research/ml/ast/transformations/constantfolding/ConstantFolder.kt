package org.jetbrains.research.ml.ast.transformations.constantfolding

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyFile
import org.jetbrains.research.ml.ast.transformations.PyUtils
import org.jetbrains.research.ml.ast.transformations.createBinaryOperandList
import org.jetbrains.research.ml.ast.transformations.createBoolLiteralExpression
import org.jetbrains.research.ml.ast.transformations.createExpressionFromNumber
import org.jetbrains.research.ml.ast.transformations.createPrefixExpression
import kotlin.collections.listOfNotNull
import kotlin.test.fail

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
            is PyEvaluatorImproved.PyIntLike -> { -> expression.replace(createIntOrBoolExpression(generator, result)) }
            is PyEvaluatorImproved.PyString ->
                { -> expression.replace(generator.createStringLiteralFromString(result.string)) }
            is PyEvaluatorImproved.PySequence -> run {
                val emptyLiteral = when (result.kind ?: return@run null) {
                    PyEvaluatorImproved.PySequence.PySequenceKind.LIST -> generator.createListLiteral()
                    PyEvaluatorImproved.PySequence.PySequenceKind.TUPLE ->
                        generator.createExpressionFromText(LanguageLevel.getDefault(), "()")
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
            is PyEvaluatorImproved.PyOperandSequence -> run {
                val simplifyUnevaluated =
                    result.unevaluatedAtoms.map { simplifyAllSubexpressionsDelayed(it.expression) }
                return@run {
                    val valueOperand =
                        listOfNotNull(result.evaluatedValue?.let { createIntOrBoolExpression(generator, it) })
                    val newExpression = PyUtils.braceExpression(
                        generator.createBinaryOperandList(
                            result.operator,
                            simplifyUnevaluated
                                .map { it() as PyExpression }
                                .mapIndexed { i, expr ->
                                    if (result.unevaluatedAtoms[i].negate) {
                                        generator.createPrefixExpression(
                                            "-",
                                            PyUtils.braceExpression(expr)
                                        )
                                    } else {
                                        expr
                                    }
                                } + valueOperand

                        )
                    )
                    expression.replace(newExpression)
                }
            }
            else -> null
        }
}

private fun createIntOrBoolExpression(
    generator: PyElementGenerator,
    result: PyEvaluatorImproved.PyIntLike
): PyExpression =
    when (result) {
        is PyEvaluatorImproved.PyInt -> generator.createExpressionFromNumber(result.value)
        is PyEvaluatorImproved.PyBool -> generator.createBoolLiteralExpression(result.value)
        else -> fail("result should be of type PyInt or PyBool")
    }
