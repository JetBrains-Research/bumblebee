/*
 * Copyright (c) 2020 Birillo A., Bobrov A., Lyulina E.
 */

package org.jetbrains.research.ml.ast.transformations.deadcode

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyIfStatement
import com.jetbrains.python.psi.PyWhileStatement
import org.jetbrains.research.ml.ast.transformations.MetaDataStorage
import org.jetbrains.research.ml.ast.transformations.Transformation
import org.jetbrains.research.ml.ast.transformations.safePerform

object DeadCodeRemovalTransformation : Transformation() {
    override val key: String = "DeadCodeRemoval"

    override fun apply(psiTree: PsiElement, metaDataStorage: MetaDataStorage?) {
        val heuristicVisitor = DeadCodeRemovalHeuristicVisitor(metaDataStorage)
        val ifStatements = PsiTreeUtil.collectElementsOfType(psiTree, PyIfStatement::class.java)
        val whileStatements = PsiTreeUtil.collectElementsOfType(psiTree, PyWhileStatement::class.java)

        WriteCommandAction.runWriteCommandAction(psiTree.project) {
            for (statement in ifStatements) {
                statement.accept(heuristicVisitor)
            }
            for (statement in whileStatements) {
                statement.accept(heuristicVisitor)
            }
        }

        val cfgVisitor = DeadCodeRemovalCFGVisitor()
        psiTree.accept(cfgVisitor)

        for (unreachable in cfgVisitor.unreachableElements) {
            WriteCommandAction.runWriteCommandAction(psiTree.project) {
//                unreachable.delete()
                metaDataStorage.safePerform({ unreachable.delete() }, "Delete unreachable element")
            }
        }
    }
}
