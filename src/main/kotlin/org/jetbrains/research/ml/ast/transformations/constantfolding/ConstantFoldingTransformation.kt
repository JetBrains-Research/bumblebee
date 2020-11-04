package org.jetbrains.research.ml.ast.transformations.constantfolding

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyElementGenerator
import org.jetbrains.research.ml.ast.transformations.Transformation

object ConstantFoldingTransformation : Transformation {
    override val metadataKey: String
        get() = TODO("Not yet implemented")

    override fun inverseApply(psiTree: PsiElement) {
        TODO("Not yet implemented")
    }

    override fun apply(psiTree: PsiElement, toStoreMetadata: Boolean) {
        val project = psiTree.project
        val folder = ConstantFolder(PyElementGenerator.getInstance(project))
        val simplify = folder.simplifyAllSubexpressionsDelayed(psiTree)
        WriteCommandAction.runWriteCommandAction(project) { simplify() }
    }
}
