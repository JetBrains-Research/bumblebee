package org.jetbrains.research.ml.ast.transformations.deadcode

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyRecursiveElementVisitor
import com.jetbrains.python.psi.PyStatementList
import org.jetbrains.research.ml.ast.storage.MetaDataStorage
import org.jetbrains.research.ml.ast.transformations.Transformation

class DeadCodeRemovalTransformation(private val storage: MetaDataStorage) : Transformation {
    override val metadataKey: String = "DeadCodeRemoval"

    override fun apply(psiTree: PsiElement, toStoreMetadata: Boolean) {
        val heuristicVisitor = DeadCodeRemovalHeuristicVisitor()
        WriteCommandAction.runWriteCommandAction(psiTree.project) {
            psiTree.accept(heuristicVisitor)
        }

        val cfgVisitor = DeadCodeRemovalCFGVisitor()
        psiTree.accept(cfgVisitor)

        for (unreachable in cfgVisitor.unreachableElements) {
            if (toStoreMetadata) {
                storeMetaDataForElement(unreachable)
            }

            WriteCommandAction.runWriteCommandAction(psiTree.project) {
                unreachable.delete()
            }
        }


        val recoverVisitor = RecoverEmptyStatementListVisitor(PyElementGenerator.getInstance(psiTree.project))
        WriteCommandAction.runWriteCommandAction(psiTree.project) {
            psiTree.accept(recoverVisitor)
        }
    }

    override fun inverseApply(psiTree: PsiElement) {
        val pyGenerator = PyElementGenerator.getInstance(psiTree.project)
        val visitor = DeadCodeRemovalInverseVisitor(pyGenerator, storage)
        WriteCommandAction.runWriteCommandAction(psiTree.project) {
            psiTree.accept(visitor)
        }
    }


    private fun storeMetaDataForElement(element: PsiElement) {
        val neighbors = storage.getMetaData(element.parent, DeadCodeRemovalStorageKeys.NODE) ?: listOf()
        storage.setMetaData(element.parent, DeadCodeRemovalStorageKeys.NODE, neighbors.plus(element.text))
    }

    private class RecoverEmptyStatementListVisitor(private val pyGenerator: PyElementGenerator) :
        PyRecursiveElementVisitor() {
        override fun visitPyStatementList(node: PyStatementList?) {
            require(node != null)
            if (node.statements.isEmpty()) {
                node.add(pyGenerator.createPassStatement())
            }
            super.visitPyStatementList(node)
        }

    }
}
