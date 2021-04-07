/*
 * Copyright (c) 2020 Birillo A., Bobrov A., Lyulina E.
 */

package org.jetbrains.research.ml.ast.transformations.deadcode

import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyIfStatement
import com.jetbrains.python.psi.PyWhileStatement
import com.jetbrains.python.psi.impl.PyEvaluator
import org.jetbrains.research.ml.ast.transformations.IPerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.PerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.PyUtils
import org.jetbrains.research.ml.ast.transformations.safePerformCommand

internal class DeadCodeRemovalHeuristicVisitor(private val commandsStorage: IPerformedCommandStorage?) :
    PyElementVisitor() {
    override fun visitPyIfStatement(node: PyIfStatement) {
        handleIfFalseStatement(node)
        super.visitPyIfStatement(node)
    }

    override fun visitPyWhileStatement(node: PyWhileStatement) {
        handleWhileFalseStatement(node)
        super.visitPyWhileStatement(node)
    }

    /**
     * Handles `if False:` case
     */
    private fun handleIfFalseStatement(node: PyIfStatement) {
        while (node.ifPart.condition?.evaluateBoolean() == false) {
            val firstElsePart = node.elifParts.firstOrNull()
            if (firstElsePart != null) {
                val newIfPart = PyUtils.createPyIfElsePart(firstElsePart)
                commandsStorage.safePerformCommand(
                    { node.ifPart.replace(newIfPart) },
                    "Replace false condition from \"if\"-node with condition from first \"elif\"-node"
                )
                commandsStorage.safePerformCommand({ firstElsePart.delete() }, "Delete first \"elif\"-node")
            } else {
                commandsStorage.safePerformCommand({ node.delete() }, "Delete \"if\"-node with false condition")
                break
            }
        }

        for (ifElsePart in node.elifParts) {
            if (ifElsePart.condition?.evaluateBoolean() == false) {
                commandsStorage.safePerformCommand({ ifElsePart.delete() }, "Delete \"else\"-node with false condition")
            }
        }
    }

    /**
     * Handles `while False:` case
     */
    private fun handleWhileFalseStatement(node: PyWhileStatement) {
        if (node.whilePart.condition?.evaluateBoolean() == false) {
            commandsStorage.safePerformCommand({ node.delete() }, "Delete \"while\"-node with false condition")
        }
    }
}

private fun PyExpression.evaluateBoolean(): Boolean? = PyEvaluator.evaluateAsBoolean(this)
