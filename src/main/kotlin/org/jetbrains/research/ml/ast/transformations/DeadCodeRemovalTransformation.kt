package org.jetbrains.research.ml.ast.transformations

import com.intellij.codeInsight.controlflow.ControlFlowUtil
import com.intellij.codeInsight.controlflow.Instruction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.jetbrains.python.codeInsight.controlflow.ControlFlowCache
import com.jetbrains.python.codeInsight.controlflow.ScopeOwner
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyRecursiveElementVisitor
import org.jetbrains.research.ml.ast.storage.MetaDataStorage
import org.jetbrains.research.ml.ast.storage.StorageKey

class DeadCodeRemovalTransformation(private val storage: MetaDataStorage) : Transformation {
    private object StorageKeys {
        val NODE = StorageKey<List<String>>("Node")
    }

    override val metadataKey: String = "DeadCodeRemoval"

    override fun apply(psiTree: PsiElement, toStoreMetadata: Boolean) {
        val visitor = ForwardVisitor()
        psiTree.accept(visitor)
        for (unreachable in visitor.unreachableElements) {
            if (toStoreMetadata) {
                val neighbors = storage.getMetaData(unreachable.parent, StorageKeys.NODE) ?: listOf()
                storage.setMetaData(unreachable.parent, StorageKeys.NODE, neighbors.plus(unreachable.text))
            }
            WriteCommandAction.runWriteCommandAction(psiTree.project) {
                unreachable.delete()
            }
        }
    }

    override fun inverseApply(psiTree: PsiElement) {
        val pyGenerator = PyElementGenerator.getInstance(psiTree.project)
        val visitor = InverseVisitor(pyGenerator, storage)
        WriteCommandAction.runWriteCommandAction(psiTree.project) {
            psiTree.accept(visitor)
        }
    }

    private class ForwardVisitor : PyRecursiveElementVisitor() {
        val unreachableElements = mutableListOf<PsiElement>()

        override fun visitElement(element: PsiElement) {
            if (element is ScopeOwner) {
                val flow = ControlFlowCache.getControlFlow(element)
                val instructions = flow.instructions
                val unreachableInstructions = mutableListOf<Instruction>()
                val unreachableInstructionsNums = mutableSetOf<Int>()
                if (instructions.isNotEmpty()) {
                    ControlFlowUtil.iteratePrev(instructions.size - 1, instructions) { instruction ->
                        if (isUnreachable(instruction, unreachableInstructionsNums)) {
                            val newUnreachable =
                                collectAllUnreachableInstructionsFrom(instruction, unreachableInstructionsNums)
                            unreachableInstructions.addAll(newUnreachable)
                        }
                        ControlFlowUtil.Operation.NEXT
                    }
                }
                unreachableElements.addAll(unreachableInstructions.mapNotNull { it.element })
            }
            super.visitElement(element)
        }

        private fun collectAllUnreachableInstructionsFrom(
            instruction: Instruction,
            unreachableInstructionsNums: MutableSet<Int>
        ): List<Instruction> {
            unreachableInstructionsNums.add(instruction.num())
            val succUnreachable = instruction.allSucc()
                .filter { isUnreachable(it, unreachableInstructionsNums) }
                .flatMap { collectAllUnreachableInstructionsFrom(it, unreachableInstructionsNums) }
            return listOf(instruction) + succUnreachable
        }

        companion object {
            fun isUnreachable(instruction: Instruction, alreadyUnreachable: MutableSet<Int>): Boolean {
                val isFirstInstruction = instruction.num() == 0
                return instruction.allPred().filterNot { alreadyUnreachable.contains(it.num()) }
                    .isEmpty() && !isFirstInstruction
            }
        }
    }

    private class InverseVisitor(private val pyGenerator: PyElementGenerator, private val storage: MetaDataStorage) :
        PyRecursiveElementVisitor() {
        override fun visitElement(element: PsiElement) {
            val unreachableElementTexts = storage.getMetaData(element, StorageKeys.NODE)
            val unreachableElements = unreachableElementTexts?.map {
                pyGenerator.createFromText(
                    LanguageLevel.PYTHON36,
                    PsiElement::class.java,
                    it
                )
            }
            if (unreachableElements != null) {
                for (unreachable in unreachableElements) {
                    element.add(unreachable)
                }
            }
            super.visitElement(element)
        }
    }
}
