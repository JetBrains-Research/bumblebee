/*
 * Copyright (c) 2020 Birillo A., Bobrov A., Lyulina E.
 */

package org.jetbrains.research.ml.ast.transformations.deadcode

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyRecursiveElementVisitor
import com.jetbrains.python.psi.PyStatementList
import org.jetbrains.research.ml.ast.transformations.Transformation

class DeadCodeRemovalTransformation : Transformation {
    override val metadataKey: String = "DeadCodeRemoval"

    override fun apply(psiTree: PsiElement, toStoreMetadata: Boolean) {
        val heuristicVisitor = DeadCodeRemovalHeuristicVisitor()
        WriteCommandAction.runWriteCommandAction(psiTree.project) {
            psiTree.accept(heuristicVisitor)
        }

        val cfgVisitor = DeadCodeRemovalCFGVisitor()
        psiTree.accept(cfgVisitor)

        for (unreachable in cfgVisitor.unreachableElements) {
            WriteCommandAction.runWriteCommandAction(psiTree.project) {
                unreachable.delete()
            }
        }
    }

    override fun inverseApply(psiTree: PsiElement) {
        TODO("Implement inverse transformation")
    }
}
