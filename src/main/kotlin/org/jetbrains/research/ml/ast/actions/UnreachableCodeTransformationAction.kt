package org.jetbrains.research.ml.ast.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.components.ServiceManager
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.impl.PythonASTFactory
import org.jetbrains.research.ml.ast.transformations.DeadCodeRemovalTransformation

class UnreachableCodeTransformationAction: AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val psiFile = event.getData(LangDataKeys.PSI_FILE) ?: return
        val pythonGenerator = PyElementGenerator.getInstance(project)
        val transformation = DeadCodeRemovalTransformation(project, pythonGenerator)
        transformation.apply(psiFile, true)
    }
}