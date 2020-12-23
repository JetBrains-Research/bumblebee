package org.jetbrains.research.ml.ast.transformations.ifRedundantLinesRemoval

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyFile
import org.jetbrains.research.ml.ast.transformations.PerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.Transformation

object IfRedundantLinesRemovalTransformation : Transformation() {
    override val key: String = "IfRedundantLinesRemoval"

    override fun forwardApply(psiTree: PsiElement, commandsStorage: PerformedCommandStorage?) {
        val project = psiTree.project
        val remover = IfRedundantLinesRemover(
            commandsStorage,
            PyElementGenerator.getInstance(project),
            psiTree.containingFile as PyFile
        )
        val simplify = remover.simplifyAllDelayed(psiTree)
        WriteCommandAction.runWriteCommandAction(project) { simplify() }
    }
}
