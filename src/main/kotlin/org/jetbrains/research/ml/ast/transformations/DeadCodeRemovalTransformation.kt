package org.jetbrains.research.ml.ast.transformations

import com.intellij.codeInsight.controlflow.ControlFlowUtil
import com.intellij.codeInsight.controlflow.Instruction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.python.codeInsight.controlflow.ControlFlowCache
import com.jetbrains.python.codeInsight.controlflow.ScopeOwner
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyRecursiveElementVisitor

class DeadCodeRemovalTransformation(
    private val project: Project,
    private val generator: PyElementGenerator
) : Transformation {
    private val unreachableElements = mutableListOf<PsiElement>()

    override val metadataKey: String = "DeadCodeRemoval"

    override fun apply(psiTree: PsiElement, toStoreMetadata: Boolean) {
        val visitor = Visitor()
        psiTree.accept(visitor)
        for (unreachable in unreachableElements) {
            val comment = PyUtils.commentElement(generator, unreachable)
            WriteCommandAction.runWriteCommandAction(project) {
                unreachable.replace(comment)
            }
        }
    }

    override fun inverseApply(psiTree: PsiElement) {
        TODO("Not yet implemented")
    }

    private inner class Visitor : PyRecursiveElementVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element is ScopeOwner) {
                val flow = ControlFlowCache.getControlFlow(element)
                val instructions = flow.instructions
                val unreachable = mutableListOf<PsiElement>()
                if (instructions.isNotEmpty()) {
                    ControlFlowUtil.iteratePrev(instructions.size - 1, instructions) { instruction ->
                        val isFirstInstruction = instruction.num() == 0
                        val instructionElement = instruction.element
                        if (instruction.allPred().isEmpty() && instructionElement != null && !isFirstInstruction) {
                            unreachable.add(instructionElement)
                            // FIXME: change
                            val nextInstructions = collectAllNextInstructions(instruction)
                            unreachable.addAll(nextInstructions.mapNotNull { it.element })

                            return@iteratePrev ControlFlowUtil.Operation.CONTINUE
                        }
                        ControlFlowUtil.Operation.NEXT
                    }
                }
                unreachableElements.addAll(unreachable)
            }
            super.visitElement(element)
        }
    }

    private fun collectAllNextInstructions(instruction: Instruction): List<Instruction> {
        val nextInstructions = mutableListOf<Instruction>()
        for (next in instruction.allSucc()) {
            val newNextInstructions = collectAllNextInstructions(next)
            nextInstructions.add(next)
            nextInstructions.addAll(newNextInstructions)
        }
        return nextInstructions
    }
}