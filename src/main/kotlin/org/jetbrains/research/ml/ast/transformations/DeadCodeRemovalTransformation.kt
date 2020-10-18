package org.jetbrains.research.ml.ast.transformations

import com.intellij.codeInsight.controlflow.ControlFlowUtil
import com.intellij.codeInsight.controlflow.Instruction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.jetbrains.python.codeInsight.controlflow.ControlFlowCache
import com.jetbrains.python.codeInsight.controlflow.ScopeOwner
import com.jetbrains.python.psi.PyRecursiveElementVisitor

class DeadCodeRemovalTransformation : Transformation {
    override val metadataKey: String = "DeadCodeRemoval"

    override fun apply(psiTree: PsiElement, toStoreMetadata: Boolean) {
        val visitor = Visitor()
        psiTree.accept(visitor)
        for (unreachable in visitor.unreachableElements) {
            WriteCommandAction.runWriteCommandAction(psiTree.project) {
                unreachable.delete()
            }
        }
    }

    override fun inverseApply(psiTree: PsiElement) {
        TODO("Not yet implemented")
    }

    private class Visitor : PyRecursiveElementVisitor() {
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
}
