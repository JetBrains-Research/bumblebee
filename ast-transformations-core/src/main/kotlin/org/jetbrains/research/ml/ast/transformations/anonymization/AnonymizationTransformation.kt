package org.jetbrains.research.ml.ast.transformations.anonymization

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyRecursiveElementVisitor
import org.jetbrains.research.ml.ast.transformations.InversableTransformation

class AnonymizationTransformation : InversableTransformation() {
    override val key: String = "Anonymization"

    private lateinit var allRenames: Map<String, String>

    override fun forwardApply(psiTree: PsiElement) {
        val visitor = AnonymizationVisitor()
        psiTree.accept(visitor)
        val renames = visitor.collectedRenames()
        allRenames = renames.map { it.value to it.key.name!! }.toMap()
        performAllRenames(psiTree.project, renames)
    }

    override fun inverseApply(psiTree: PsiElement) {
        // bug in intellij with buildin 5. PyCharm thinks that print is reserved name.
        val existedRenames = allRenames
        val originRenames = mutableMapOf<PyElement, String>()
        psiTree.accept(object : PyRecursiveElementVisitor() {
            override fun visitPyElement(node: PyElement) {
                if (node.isDefinition()) {
                    node.name?.also { nodeName ->
                        existedRenames[nodeName]?.also { originalName ->
                            if (!originRenames.containsValue(originalName)) {
                                originRenames[node] = originalName
                            }
                        }
                    }
                }
                super.visitPyElement(node)
            }
        })
        performAllRenames(psiTree.project, originRenames)
    }

    private fun performAllRenames(project: Project, allRenames: Map<PyElement, String>) {
        val renames = allRenames.map { RenameUtil.renameElementDelayed(it.key, it.value) }
        for (rename in renames)
            WriteCommandAction.runWriteCommandAction(project, rename)
    }
}
