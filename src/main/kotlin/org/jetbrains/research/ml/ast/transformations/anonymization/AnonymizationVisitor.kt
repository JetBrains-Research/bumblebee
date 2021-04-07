package org.jetbrains.research.ml.ast.transformations.anonymization

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyRecursiveElementVisitor
import org.jetbrains.research.ml.ast.transformations.IPerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.PerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.safePerformCommand

class AnonymizationVisitor(file: PyFile) : PyRecursiveElementVisitor() {
    private val project = file.project
    private val anonymizer = ElementAnonymizer()

    override fun visitElement(element: PsiElement) {
        anonymizer.registerElement(element)
        super.visitElement(element)
    }

    fun performAllRenames(commandsStorage: IPerformedCommandStorage?) {
        val allRenames = anonymizer.getAllRenames()

        val redoRenames = allRenames.map { RenameUtil.renameElementDelayed(it.first, it.second) }
//        val oldNames = allRenames.map { (it.first as PsiNamedElement).name!! }
//        val undoRenames = allRenames.mapIndexed { i, it -> RenameUtil.renameElementDelayed(it.first, oldNames[i]) }
        val undoRenames = allRenames.map {  RenameUtil.renameElementDelayed(it.first, "testName") }

        WriteCommandAction.runWriteCommandAction(project) {
//            renames.forEach { commandsStorage.safePerformCommand(it, "Anonymize element") }
            redoRenames.zip(undoRenames).forEach { (redo, undo) ->
                commandsStorage?.performUndoableCommand(redo, undo, "Anonymize element")
            }
        }
    }
}
