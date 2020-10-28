package org.jetbrains.research.ml.ast.transformations.multipleTargetAssignment

import com.jetbrains.python.psi.PyAssignmentStatement
import com.jetbrains.python.psi.PyRecursiveElementVisitor
import org.jetbrains.research.ml.ast.transformations.PyUtils

class MultipleTargetAssignmentVisitor : PyRecursiveElementVisitor() {
    override fun visitPyAssignmentStatement(node: PyAssignmentStatement?) {
        if (node != null) {
            processAssignment(node)
        }
        super.visitPyAssignmentStatement(node)
    }

    private fun processAssignment(node: PyAssignmentStatement) {
        val targets = node.targets.toList()
        val assignedValue = node.assignedValue ?: return
        val firstTarget = targets.first()
        node.parent.addBefore(PyUtils.createAssignment(firstTarget, assignedValue), node)
        for ((value, target) in targets.windowed(2)) {
            node.parent.addBefore(PyUtils.createAssignment(target, value), node)
        }
        node.delete()
    }
}
