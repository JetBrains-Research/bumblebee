package org.jetbrains.research.ml.ast.transformations

import com.intellij.codeInsight.editorActions.smartEnter.SmartEnterProcessor.commitDocument
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import java.util.logging.Logger

// Todo: rename?
class MetaDataStorage(private val psiTree: PsiElement) {
    private val project: Project = psiTree.project
    private val logger = Logger.getLogger(javaClass.name)
    private val commandProcessor = CommandProcessor.getInstance()
    private var commandDescriptions = ArrayDeque<String>()

//    Should be run in WriteAction
    fun perform(command: () -> Unit, description: String) {
        commandDescriptions.addLast(description)
        commandProcessor.executeCommand(
            project,
            command,
            description,
            null
        )
    }

    fun undoCommands(): PsiElement {
        val file = psiTree.containingFile.virtualFile
        val doc = FileDocumentManager.getInstance().getDocument(file)!!
        val editor = EditorFactory.getInstance().createEditor(doc, project)!!
        val fileEditor = TextEditorProvider.getInstance().getTextEditor(editor)
        val manager = UndoManager.getInstance(project)

        while (commandDescriptions.isNotEmpty()) {
            val description = commandDescriptions.removeLast()
            if (manager.isUndoAvailable(fileEditor)) {
//              We need to have try-catch when we undo commands on modified tree, because some of them cannot be performed {
                try {
                    manager.undo(fileEditor)
                } catch (e: Exception) {
                    logger.info("Command $description failed to be undone")
                }
            } else {
                logger.info("Command $description is unavailable to undo")
            }
        }
        EditorFactory.getInstance().releaseEditor(editor)
        commitDocument(editor)
        return PsiManager.getInstance(project).findFile(file)!!
    }
}

fun MetaDataStorage?.safePerform(command: () -> Unit, description: String) {
    this?.perform(command, description) ?: command()
}
