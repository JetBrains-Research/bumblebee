package org.jetbrains.research.ml.ast.transformations.constantfolding

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyFile
import org.jetbrains.research.ml.ast.transformations.PerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.Transformation

object ConstantFoldingTransformation : Transformation() {
    override val key: String = "ConstantFolding"

    override fun forwardApply(psiTree: PsiElement, commandsStorage: PerformedCommandStorage?) {
        val project = psiTree.project
        val folder =
            ConstantFolder(commandsStorage, PyElementGenerator.getInstance(project), psiTree.containingFile as PyFile)
        val simplify = folder.simplifyAllSubexpressionsDelayed(psiTree)
        WriteCommandAction.runWriteCommandAction(project) { simplify() }
    }
}
