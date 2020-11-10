package org.jetbrains.research.ml.ast.transformations.augmentedAssignment

import com.jetbrains.python.psi.PyAugAssignmentStatement
import com.jetbrains.python.psi.PyRecursiveElementVisitor
import org.jetbrains.research.ml.ast.transformations.PyUtils

internal class AugmentedAssignmentVisitor : PyRecursiveElementVisitor() {
    override fun visitPyAugAssignmentStatement(node: PyAugAssignmentStatement?) {
        if (node != null) {
            handleAugAssignment(node)
        }
        super.visitPyAugAssignmentStatement(node)
    }

    private fun handleAugAssignment(node: PyAugAssignmentStatement) {
        val newAssignment = PyUtils.createAssignment(node)
        node.replace(newAssignment)
    }
}
