package org.jetbrains.research.ml.ast.transformations.anonymization

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyRecursiveElementVisitor
import org.jetbrains.research.ml.ast.transformations.commands.Command
import org.jetbrains.research.ml.ast.transformations.commands.ICommandPerformer
import org.jetbrains.research.ml.ast.transformations.commands.RenamablePsiElement
import org.jetbrains.research.ml.ast.transformations.commands.RenameCommand

class AnonymizationVisitor(file: PyFile) : PyRecursiveElementVisitor() {
    private val project = file.project
    private val anonymizer = ElementAnonymizer()

    override fun visitElement(element: PsiElement) {
        anonymizer.registerElement(element)
        super.visitElement(element)
    }

    fun performAllRenames(commandsPerformer: ICommandPerformer) {
        val allRenames = anonymizer.getAllRenames()

        val commands = allRenames.map { (psi, newName) -> RenamablePsiElement(psi, newName) }
        commands.forEach { commandsPerformer.performCommand(RenameCommand.getCommand(it, "Anonymize element")) }
//
//        allRenames.map { (psi, newName) -> RenamablePsiElement(psi as PsiNamedElement, newName) }.forEach {
//                commandsPerformer.performCommand(RenameCommand.getCommand(it, "Anonymize element"))
//        }

//        allRenames.forEach { (psi, newName) ->
//            val renamablePsiElement = RenamablePsiElement(psi as PsiNamedElement, newName)
//            WriteCommandAction.runWriteCommandAction(project) {
//                commandsPerformer.performCommand(RenameCommand.getCommand(renamablePsiElement, "Anonymize element"))
//            }
//        }

//        val redoRenames = allRenames.map {
//            RenameUtil.renameElementDelayed(it.first, it.second)
//        }

//        val oldNames = allRenames.map { (it.first as PsiNamedElement).name!! }
//        val undoRenames = allRenames.mapIndexed { i, it -> RenameUtil.renameElementDelayed(it.first, oldNames[i]) }

//        WriteCommandAction.runWriteCommandAction(project) {
//            redoRenames.forEach { redo ->
//                commandsPerformer.performCommand(Command(redo, { }, "Anonymize element"))
//            }
//        }
    }
}
