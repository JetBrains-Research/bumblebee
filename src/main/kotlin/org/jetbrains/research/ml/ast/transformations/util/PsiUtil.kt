package org.jetbrains.research.ml.ast.transformations.util

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor

object PsiUtil {
    fun acceptStatements(project: Project, statements: Collection<PsiElement>, visitor: PsiElementVisitor) {
        WriteCommandAction.runWriteCommandAction(project) {
            for (statement in statements) {
                statement.accept(visitor)
            }
        }
    }
}
