package org.jetbrains.research.ml.ast.transformations.augmentedAssignment

import com.jetbrains.python.psi.PyAugAssignmentStatement
import com.jetbrains.python.psi.PyElementVisitor
import org.jetbrains.research.ml.ast.transformations.IPerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.PerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.PyUtils
import org.jetbrains.research.ml.ast.transformations.safePerformCommand

internal class AugmentedAssignmentVisitor(private val commandsStorage: IPerformedCommandStorage?) : PyElementVisitor() {
    override fun visitPyAugAssignmentStatement(node: PyAugAssignmentStatement) {
        handleAugAssignment(node)
        super.visitPyAugAssignmentStatement(node)
    }

    private fun handleAugAssignment(node: PyAugAssignmentStatement) {
        val newAssignment = PyUtils.createAssignment(node)
        commandsStorage.safePerformCommand({ node.replace(newAssignment) }, "Replace AugAssignment")
    }
}
