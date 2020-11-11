/*
 * Copyright (c) 2020 Birillo A., Bobrov A., Lyulina E.
 */

package org.jetbrains.research.ml.ast.transformations.deadcode

import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyEvaluator
import org.jetbrains.research.ml.ast.transformations.PyUtils

internal class DeadCodeRemovalHeuristicVisitor : PyElementVisitor() {
    override fun visitPyIfStatement(node: PyIfStatement?) {
        if (node != null) {
            handleIfFalseStatement(node)
        }
        super.visitPyIfStatement(node)
    }

    override fun visitPyWhileStatement(node: PyWhileStatement?) {
        if (node != null) {
            handleWhileFalseStatement(node)
        }
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
