package org.jetbrains.research.ml.ast.transformations

import com.intellij.codeInsight.editorActions.smartEnter.SmartEnterProcessor.commitDocument
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.undo.BasicUndoableAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.command.undo.UndoableAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import java.util.logging.Logger

interface IPerformedCommandStorage {
    val psiTree: PsiElement
    fun performCommand(command: () -> Unit, description: String)
    fun undoPerformedCommands(maxN: Int = Int.MAX_VALUE): PsiElement
    fun performUndoableCommand(command: () -> Unit, undoCommand: () -> Unit, description: String)
}

class PerformedCommandStorage(override val psiTree: PsiElement) : IPerformedCommandStorage {
    private val project: Project = psiTree.project
    private val logger = Logger.getLogger(javaClass.name)

    private val commandProcessor = CommandProcessor.getInstance()
    private var commandDescriptions = ArrayDeque<String>()
    private var commands = ArrayDeque<UndoableAction>()

    private val file: VirtualFile = psiTree.containingFile.virtualFile
    private val document: Document = FileDocumentManager.getInstance().getDocument(file)!!

    inner class UndoPerformer {
        val editor: Editor = EditorFactory.getInstance().createEditor(document, project)!!
        val fileEditor = TextEditorProvider.getInstance().getTextEditor(editor)
        val manager: UndoManager = UndoManager.getInstance(project)

        fun releaseEditor() {
            EditorFactory.getInstance().releaseEditor(editor)
        }
    }

    //    Should be run in WriteAction
    override fun performCommand(command: () -> Unit, description: String) {
        commandDescriptions.addLast(description)
        commandProcessor.executeCommand(
            project,
            command,
            description,
            null
        )
    }

    override fun performUndoableCommand(command: () -> Unit, undoCommand: () -> Unit, description: String) {
        commandDescriptions.addLast(description)
        commandProcessor.executeCommand(
            project,
            command,
            description,
            null
        )

        commands.addLast(object : BasicUndoableAction(psiTree.containingFile.virtualFile) {
            override fun undo() = undoCommand()
            override fun redo() = command()
        })

        FileDocumentManager.getInstance().saveDocument(document)
    }

    private fun undoLastCommand(undoPerformer: UndoPerformer) {
        if (commandDescriptions.isNotEmpty()) {
            val description = commandDescriptions.removeLast()
            if (undoPerformer.manager.isUndoAvailable(undoPerformer.fileEditor)) {
//              We need to have try-catch block when we undo commands on modified tree
//              because some of them cannot be undone
                try {
//                  Got an error "Document is locked by write PSI operations.
//                  Use PsiDocumentManager.doPostponedOperationsAndUnblockDocument() to commit PSI changes to the document."
                    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)

//                    undoPerformer.manager.undo(undoPerformer.fileEditor)
                    commandProcessor.executeCommand(
                        project,
                        commands.removeLast()::undo,
                        description,
                        null
                    )
                } catch (e: Exception) {
                    logger.info("Command $description failed to be undone")
                }
            } else {
                logger.info("Command $description is unavailable to undo")
            }
        }

        FileDocumentManager.getInstance().saveDocument(document)
    //      Should I commit the document?
    //        commitDocument(undoPerformer.editor)

    }

    override fun undoPerformedCommands(maxN: Int): PsiElement {
        val undoPerformer = UndoPerformer()
        var n = 0
        while (n < maxN && commandDescriptions.isNotEmpty()) {
            undoLastCommand(undoPerformer)
            n++
        }
        undoPerformer.releaseEditor()
        return PsiManager.getInstance(project).findFile(file)!!
    }
}

fun IPerformedCommandStorage?.safePerformCommand(command: () -> Unit, description: String) {
    this?.performCommand(command, description) ?: command()
}

fun IPerformedCommandStorage?.safePerformUndoableCommand(
    command: () -> Unit,
    undoCommand: () -> Unit,
    description: String
) {
    this?.performUndoableCommand(command, undoCommand, description) ?: command()
}

fun <R> IPerformedCommandStorage?.safePerformCommandWithResult(command: () -> R, description: String): R {
    var result: R? = null
    this.safePerformCommand({ result = command() }, description)
    return result!!
}
