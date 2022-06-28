package org.jetbrains.research.ml.ast.util

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import java.io.File

class PsiFileHandler(private val fixture: CodeInsightTestFixture, private val project: Project) {
    private val codeStyleManager = CodeStyleManager.getInstance(project)

    fun getPsiFile(file: File, toReformatFile: Boolean = true): PsiFile {
        val psiFile = fixture.configureByFile(file.path)
        if (toReformatFile) {
            formatPsiFile(psiFile)
        }
        return psiFile
    }

    fun formatPsiFile(psi: PsiElement) {
        WriteCommandAction.runWriteCommandAction(project) {
            codeStyleManager.reformat(psi)
        }
    }
}

fun CodeInsightTestFixture.getPsiFile(file: File): PsiFile = this.configureByFile(file.path)
