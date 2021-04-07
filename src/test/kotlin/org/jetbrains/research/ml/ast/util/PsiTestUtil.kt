package org.jetbrains.research.ml.ast.util

import com.github.gumtreediff.tree.ITree
import com.github.gumtreediff.tree.TreeContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.elementType
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import org.jetbrains.research.ml.ast.gumtree.psi.label
import org.jetbrains.research.ml.ast.gumtree.psi.preOrder
import org.jetbrains.research.ml.ast.gumtree.tree.Numbering.PsiTreeUtils.Companion.id
import java.io.File

object PsiTestUtil {
    fun PsiElement.equalTreeStructure(treeCtx: TreeContext, toCompareNumbering: Boolean = true): Boolean {
        val psiPreOrder = ApplicationManager.getApplication().runReadAction<List<PsiElement>> {
            this.preOrder().toList()
        }
        val treeCtxPreOrder = treeCtx.root.preOrder().toList()

        if (psiPreOrder.size != treeCtxPreOrder.size) {
            return false
        }
        return psiPreOrder.zip(treeCtxPreOrder).all { (psi, tree) ->
            compareStructure(
                psi,
                tree,
                treeCtx,
                toCompareNumbering
            )
        }
    }

    private fun compareStructure(
        psi: PsiElement,
        tree: ITree?,
        treeCtx: TreeContext,
        toCompareNumbering: Boolean = true
    ): Boolean {
        if (tree == null) {
            return false
        }
        // Compare type
        if (psi.elementType.toString() != treeCtx.getTypeLabel(tree.type)) {
            return false
        }
        // Compare labels
        if (psi.label != tree.label) {
            return false
        }
        if (toCompareNumbering) {
            // Compare ids
            if (psi.id == null || psi.id != tree.id) {
                return false
            }
        }
        return true
    }
}

class PsiFileHandler(private val fixture: CodeInsightTestFixture, val project: Project) {
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
