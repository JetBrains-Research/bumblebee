/*
 * Copyright (c) 2020 Birillo A., Bobrov A., Lyulina E.
 */

package org.jetbrains.research.ml.ast.transformations.deadcode

import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyIfStatement
import com.jetbrains.python.psi.PyWhileStatement
import com.jetbrains.python.psi.impl.PyEvaluator
import org.jetbrains.research.ml.ast.transformations.MetaDataStorage
import org.jetbrains.research.ml.ast.transformations.PyUtils
import org.jetbrains.research.ml.ast.transformations.safePerform

internal class DeadCodeRemovalHeuristicVisitor(private val metaDataStorage: MetaDataStorage?) : PyElementVisitor() {
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
//                node.ifPart.replace(newIfPart)
                metaDataStorage.safePerform(
                    { node.ifPart.replace(newIfPart) },
                    "Replace false condition from \"if\"-node with condition from first \"elif\"-node"
                )
//                firstElsePart.delete()
                metaDataStorage.safePerform({ firstElsePart.delete() }, "Delete first \"elif\"-node")
            } else {
//                node.delete()
                metaDataStorage.safePerform({ node.delete() }, "Delete \"if\"-node with false condition")
                break
            }
        }

        for (ifElsePart in node.elifParts) {
            if (ifElsePart.condition?.evaluateBoolean() == false) {
//                ifElsePart.delete()
                metaDataStorage.safePerform({ ifElsePart.delete() }, "Delete \"else\"-node with false condition")
            }
        }
    }

    /**
     * Handles `while False:` case
     */
    private fun handleWhileFalseStatement(node: PyWhileStatement) {
        if (node.whilePart.condition?.evaluateBoolean() == false) {
//            node.delete()
            metaDataStorage.safePerform({ node.delete() }, "Delete \"while\"-node with false condition")
        }
    }
}

private fun PyExpression.evaluateBoolean(): Boolean? = PyEvaluator.evaluateAsBoolean(this)
