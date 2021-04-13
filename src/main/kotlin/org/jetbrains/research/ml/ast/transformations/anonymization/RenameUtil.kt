package org.jetbrains.research.ml.ast.transformations.anonymization

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.usageView.UsageInfo
import com.jetbrains.python.refactoring.rename.RenamePyElementProcessor

object RenameUtil {

//  Todo: maybe move it to the RenamableElement?
    fun renameElementDelayed(definition: PsiElement, newName: String): () -> Unit {
        val processor = RenamePsiElementProcessor.forElement(definition)
        val allRenames = mutableMapOf(definition to newName)
        processor.prepareRenaming(definition, newName, allRenames)
        val delayedRenames = allRenames.map { renameSingleElementDelayed(it.key, it.value) }
        return { delayedRenames.forEach { it() } }
    }

    private fun renameSingleElementDelayed(definition: PsiElement, newName: String): () -> Unit {
        val processor = RenamePsiElementProcessor.forElement(definition)
        val useScope = definition.useScope
        val references = processor.findReferences(definition, useScope, false)
        val usages = references.map { UsageInfo(it) }.toTypedArray()
        return {
            WriteCommandAction.runWriteCommandAction(definition.project) {
                processor.renameElement(definition, newName, usages, null)
            }
        }
    }
}
