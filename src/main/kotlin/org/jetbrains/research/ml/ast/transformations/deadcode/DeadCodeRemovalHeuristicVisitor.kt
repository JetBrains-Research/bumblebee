package org.jetbrains.research.ml.ast.transformations.deadcode

import com.jetbrains.python.psi.PyIfStatement
import com.jetbrains.python.psi.PyRecursiveElementVisitor
import com.jetbrains.python.psi.PyWhileStatement
import com.jetbrains.python.psi.impl.PyEvaluator
import org.jetbrains.research.ml.ast.transformations.PyUtils

internal class DeadCodeRemovalHeuristicVisitor : PyRecursiveElementVisitor() {
    override fun visitPyIfStatement(node: PyIfStatement?) {
        if (node != null) {
            /**
             * Handles `if False:` case
             */

            while (PyEvaluator.evaluateAsBoolean(node.ifPart.condition) == false) {
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
                val evaluationResult = PyEvaluator.evaluateAsBoolean(ifElsePart.condition)
                if (evaluationResult == false) {
                    ifElsePart.delete()
                }
            }
        }
        super.visitPyIfStatement(node)
    }

    override fun visitPyWhileStatement(node: PyWhileStatement?) {
        if (node != null) {
            /**
             * Handles `while False:` case
             */
            val evaluationResult = PyEvaluator.evaluateAsBoolean(node.whilePart.condition)
            if (evaluationResult == false) {
                node.delete()
            }
        }
        super.visitPyWhileStatement(node)
    }
}
