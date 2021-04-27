package org.jetbrains.research.ml.ast.transformations.augmentedAssignment

import com.jetbrains.python.psi.PyAugAssignmentStatement
import com.jetbrains.python.psi.PyElementVisitor
import org.jetbrains.research.ml.ast.transformations.PyUtils

internal class AugmentedAssignmentVisitor() : PyElementVisitor() {
    override fun visitPyAugAssignmentStatement(node: PyAugAssignmentStatement) {
        handleAugAssignment(node)
        super.visitPyAugAssignmentStatement(node)
    }

    private fun handleAugAssignment(node: PyAugAssignmentStatement) {
        val newAssignment = PyUtils.createAssignment(node)
        node.replace(newAssignment)
    }
}
