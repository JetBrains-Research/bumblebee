package org.jetbrains.research.ml.ast.transformations.anonymization

import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyFile
import org.jetbrains.research.ml.ast.transformations.Transformation
import org.jetbrains.research.ml.ast.transformations.commands.ICommandPerformer

object AnonymizationTransformation : Transformation() {
    override val key: String = "Anonymization"

    override fun forwardApply(psiTree: PsiElement, commandPerformer: ICommandPerformer) {
        psiTree.project.service<DumbService>().runReadActionInSmartMode {
            val visitor = AnonymizationVisitor(psiTree.containingFile as PyFile)
            psiTree.accept(visitor)
            visitor.performAllRenames(commandPerformer)
        }
    }
}
