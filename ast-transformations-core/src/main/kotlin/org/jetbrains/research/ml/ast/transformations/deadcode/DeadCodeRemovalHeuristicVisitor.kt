/*
 * Copyright (c) 2020 Birillo A., Bobrov A., Lyulina E.
 */

package org.jetbrains.research.ml.ast.transformations.deadcode

import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyIfStatement
import com.jetbrains.python.psi.PyWhileStatement
import com.jetbrains.python.psi.impl.PyEvaluator
import org.jetbrains.research.ml.ast.transformations.PyUtils

internal class DeadCodeRemovalHeuristicVisitor : PyElementVisitor() {
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
                node.ifPart.replace(newIfPart)
                firstElsePart.delete()
            } else {
                node.delete()
                break
            }
        }

        for (ifElsePart in node.elifParts) {
            if (ifElsePart.condition?.evaluateBoolean() == false) {
                ifElsePart.delete()
            }
        }
    }

    /**
     * Handles `while False:` case
     */
    private fun handleWhileFalseStatement(node: PyWhileStatement) {
        if (node.whilePart.condition?.evaluateBoolean() == false) {
            node.delete()
        }
    }
}

private fun PyExpression.evaluateBoolean(): Boolean? = PyEvaluator.evaluateAsBoolean(this)
