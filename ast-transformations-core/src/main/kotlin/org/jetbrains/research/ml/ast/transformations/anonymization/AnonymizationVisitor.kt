package org.jetbrains.research.ml.ast.transformations.anonymization

import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyRecursiveElementVisitor

class AnonymizationVisitor : PyRecursiveElementVisitor() {
    private val anonymizer = ElementAnonymizer()

    override fun visitPyElement(node: PyElement) {
        anonymizer.registerIfNeeded(node)
        super.visitPyElement(node)
    }

    fun collectedRenames(): Map<PyElement, String> = anonymizer.getAllRenames()
}
