package org.jetbrains.research.ml.ast.transformations.ifRedundantLinesRemoval

import com.intellij.psi.PsiElement
import com.intellij.psi.util.siblings
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyElsePart
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyIfPart
import com.jetbrains.python.psi.PyIfStatement
import com.jetbrains.python.psi.PyStatement
import org.jetbrains.research.ml.ast.transformations.PerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.PyUtils
import org.jetbrains.research.ml.ast.transformations.constantfolding.PyEvaluatorImproved
import org.jetbrains.research.ml.ast.transformations.createBinaryOperandList
import org.jetbrains.research.ml.ast.transformations.createExpressionStatement
import org.jetbrains.research.ml.ast.transformations.createIfPart
import org.jetbrains.research.ml.ast.transformations.createIfPartFromIfPart
import org.jetbrains.research.ml.ast.transformations.createPrefixExpression
import org.jetbrains.research.ml.ast.transformations.safePerformCommand
import org.jetbrains.research.ml.ast.transformations.safePerformCommandWithResult
import kotlin.test.fail

class IfRedundantLinesRemover(
    private val commandStorage: PerformedCommandStorage?,
    private val generator: PyElementGenerator,
    file: PyFile
) {
    private val evaluator = PyEvaluatorImproved(file)

    fun simplifyAllDelayed(element: PsiElement): () -> Unit {
        val simplify = (element as? PyStatement)?.let { simplifyStatementDelayed(it) }
            ?: simplifySubStatementsDelayed(element)
        return { simplify() }
    }

    private fun simplifySubStatementsDelayed(element: PsiElement): () -> Unit {
        val simplifySubStatements = element.children.map { simplifyAllDelayed(it) }
        return { simplifySubStatements.forEach { it() } }
    }

    private class StatementRange(val first: PyStatement, val last: PyStatement)

    private fun simplifyStatementDelayed(statement: PyStatement): () -> StatementRange {
        val ifStatement = statement as? PyIfStatement
        if (ifStatement != null &&
            ifStatement.elsePart != null &&
            ifStatement.ifPart.condition != null &&
            ifStatement.elifParts.all { it.condition != null }
        ) {
            val ifParts = listOf(ifStatement.ifPart) + ifStatement.elifParts
            val statementParts = ifParts + ifStatement.elsePart!!
            val statementLists = statementParts.map { it.statementList.statements.toList() }

            // Finding a common suffix is always preferred
            val suffixLength = getCommonSuffixLength(statementLists)
            val maxAllowedPrefixLength = statementLists.map { it.size - suffixLength }.minOrNull()!!
            val conditions = ifParts.map { it.condition!! }
            val allConditionsArePure = conditions.all { evaluator.canBeProvenPure(it) }
            val prefixLength = if (allConditionsArePure) {
                minOf(getCommonPrefixLength(statementLists), maxAllowedPrefixLength)
            } else 0

            // Handle all sub-statements excluding duplicates
            val ifStatements = statementLists.first()
            val simplifyPrefix = if (prefixLength > 0) {
                simplifyStatementListDelayed(ifStatements.take(prefixLength))
            } else null
            val simplifySuffix = if (suffixLength > 0) {
                simplifyStatementListDelayed(ifStatements.takeLast(suffixLength))
            } else null

            val uniqueStatementLists = statementLists.map { it.drop(prefixLength).dropLast(suffixLength) }
            val simplifyUniqueStatements = uniqueStatementLists.map {
                if (it.isNotEmpty()) simplifyStatementListDelayed(it) else null
            }
            val (partsToKeepIds, partsToRemoveIds) = statementLists.indices
                .partition { prefixLength + suffixLength < statementLists[it].size }

            return {
                // Handle common prefix and suffix
                var newFirst = if (simplifyPrefix != null) {
                    val newPrefixRange = simplifyPrefix()
                    moveStatementsRange(newPrefixRange, ifStatement, true).first
                } else ifStatement
                val newLast = if (simplifySuffix != null) {
                    val newSuffixRange = simplifySuffix()
                    moveStatementsRange(newSuffixRange, ifStatement, false).last
                } else ifStatement

                // Remove the duplicate parts from all if/else parts
                for (statementList in statementLists.drop(1)) {
                    if (prefixLength > 0) {
                        commandStorage.safePerformCommand(
                            {
                                statementList.first().parent.deleteChildRange(
                                    statementList.first(),
                                    statementList[prefixLength - 1]
                                )
                            },
                            "Remove duplicate statements"
                        )
                    }
                    if (suffixLength > 0) {
                        commandStorage.safePerformCommand(
                            {
                                statementList.last().parent.deleteChildRange(
                                    statementList[statementList.size - suffixLength],
                                    statementList.last()
                                )
                            },
                            "Remove duplicate statements"
                        )
                    }
                }

                // Remove the whole if when necessary
                // and make sure the first remaining part is always "if ...:"
                if (partsToKeepIds.isEmpty()) {
                    // All statements have been selected as parts of a suffix
                    if (allConditionsArePure) {
                        newFirst = newFirst.nextStatements().first()
                        commandStorage.safePerformCommand({ ifStatement.delete() }, "Delete redundant if statement")
                    } else {
                        val disjunction = generator.createBinaryOperandList("or", conditions)
                        val disjunctionStatement = generator.createExpressionStatement(disjunction)
                        newFirst = commandStorage.safePerformCommandWithResult(
                            {
                                ifStatement.replace(disjunctionStatement)
                            },
                            "Replace if with just the conditions"
                        ) as PyStatement
                    }
                } else {
                    val firstToKeepId = partsToKeepIds.first()
                    val firstToKeep = statementParts[firstToKeepId]
                    val simplifiedFirstRange = simplifyUniqueStatements.map { it?.let { it() } }[firstToKeepId]!!
                    val replacementPart = when (firstToKeep) {
                        is PyIfPart -> {
                            generator.createIfPartFromIfPart(firstToKeep)
                        }
                        is PyElsePart -> {
                            val disjunction = generator.createBinaryOperandList("or", conditions)
                            val notDisjunction = generator.createPrefixExpression(
                                "not",
                                PyUtils.braceExpression(disjunction)
                            )
                            generator.createIfPart(
                                notDisjunction,
                                simplifiedFirstRange.first.nextStatements()
                                    .takeWhile { it != simplifiedFirstRange.last }
                                    .toList() + simplifiedFirstRange.last
                            )
                        }
                        else -> fail("Unexpected type of if part encountered")
                    }
                    commandStorage.safePerformCommand(
                        { firstToKeep.replace(replacementPart) },
                        "Replace a part to restore if correctness"
                    )
                }

                // Remove whole parts when necessary
                for (index in partsToRemoveIds) {
                    if (conditions.getOrNull(index)?.let { evaluator.canBeProvenPure(it) } != false) {
                        commandStorage.safePerformCommand(
                            { statementParts[index].delete() },
                            "Remove redundant part of if statement"
                        )
                    } else {
                        commandStorage.safePerformCommand(
                            {
                                statementParts[index].add(generator.createPassStatement())
                            }, "Replace statements with a single 'pass' in part of if with impure condition"
                        )
                    }
                }

                StatementRange(newFirst, newLast)
            }
        }

        val simplifySubStatements = simplifySubStatementsDelayed(statement)
        return {
            simplifySubStatements()
            StatementRange(statement, statement)
        }
    }

    private fun simplifyStatementListDelayed(statements: List<PyStatement>): () -> StatementRange {
        val simplifyStatements = statements.map { simplifyStatementDelayed(it) }
        return {
            val simplifiedRanges = simplifyStatements.map { it() }
            StatementRange(simplifiedRanges.first().first, simplifiedRanges.last().last)
        }
    }

    private fun getCommonPrefixLength(statementLists: List<List<PyStatement>>): Int {
        var commonPrefixLength = 0
        while (areEqualNotNullStatements(statementLists.map { it.getOrNull(commonPrefixLength) })) {
            ++commonPrefixLength
        }
        return commonPrefixLength
    }

    private fun getCommonSuffixLength(statementLists: List<List<PyStatement>>): Int =
        getCommonPrefixLength(statementLists.map { it.reversed() })

    private fun areEqualNotNullStatements(statements: List<PyStatement?>): Boolean =
        statements.all { it != null && areStatementsEquivalent(it, statements.first()!!) }

    private fun areStatementsEquivalent(first: PyStatement, second: PyStatement): Boolean =
    // PsiEquivalenceUtil.areElementsEquivalent(first, second)
        // TODO: replace with something more sensible
        first.textMatches(second)

    private fun moveStatementsRange(
        range: StatementRange,
        anchor: PsiElement,
        before: Boolean
    ): StatementRange {
        val indexOfLast = range.first.nextStatements().indexOf(range.last)
        val anchorParent = anchor.parent
        val newFirst = commandStorage.safePerformCommandWithResult(
            {
                if (before) {
                    anchorParent.addRangeBefore(range.first, range.last, anchor)
                } else {
                    anchorParent.addRangeAfter(range.first, range.last, anchor)
                } as PyStatement
            },
            "Insert duplicate statements to new position"
        )
        val newLast = newFirst.nextStatements().elementAt(indexOfLast)
        commandStorage.safePerformCommand(
            { range.first.parent.deleteChildRange(range.first, range.last) },
            "Remove original duplicate statements"
        )
        return StatementRange(newFirst, newLast)
    }

    companion object {
        fun PyStatement.nextStatements() = siblings(forward = true, withSelf = true).filterIsInstance<PyStatement>()
    }
}
