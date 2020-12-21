package org.jetbrains.research.ml.ast.transformations.anonymization

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyFile
import org.jetbrains.research.ml.ast.transformations.PerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.Transformation

object AnonymizationTransformation : Transformation() {
    override val key: String = "Anonymization"

    override fun forwardApply(psiTree: PsiElement, commandsStorage: PerformedCommandStorage?) {
        val visitor = AnonymizationVisitor(psiTree.containingFile as PyFile)
        psiTree.accept(visitor)
        visitor.performAllRenames(commandsStorage)
    }
}
