package org.jetbrains.research.ml.ast.gumtree

import com.github.gumtreediff.tree.TreeContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiFile
import org.jetbrains.research.ml.ast.gumtree.tree.Numbering
import org.jetbrains.research.ml.ast.gumtree.tree.PsiTreeConverter

object Util {
    fun getTreeContext(psiFile: PsiFile, numbering: Numbering): TreeContext {
        return ApplicationManager.getApplication().runReadAction<TreeContext> {
            PsiTreeConverter.convertTree(psiFile, numbering)
        }
    }
}
