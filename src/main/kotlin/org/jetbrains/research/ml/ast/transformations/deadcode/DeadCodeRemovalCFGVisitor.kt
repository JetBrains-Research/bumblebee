/*
 * Copyright (c) 2020 Birillo A., Bobrov A., Lyulina E.
 */

package org.jetbrains.research.ml.ast.transformations.deadcode

import com.intellij.codeInsight.controlflow.ControlFlowUtil
import com.intellij.codeInsight.controlflow.Instruction
import com.intellij.psi.PsiElement
import com.jetbrains.python.codeInsight.controlflow.ControlFlowCache
import com.jetbrains.python.codeInsight.controlflow.ScopeOwner
import com.jetbrains.python.psi.PyRecursiveElementVisitor

internal class DeadCodeRemovalCFGVisitor : PyRecursiveElementVisitor() {
    internal val unreachableElements = mutableListOf<PsiElement>()

    override fun visitElement(element: PsiElement) {
        if (element is ScopeOwner) {
            val flow = ControlFlowCache.getControlFlow(element)
            val instructions = flow.instructions
            val unreachableInstructions = mutableListOf<Instruction>()
            val unreachableInstructionsNums = mutableSetOf<Int>()
            if (instructions.isNotEmpty()) {
                ControlFlowUtil.iteratePrev(instructions.size - 1, instructions) { instruction ->
                    if (instruction.isUnreachable(unreachableInstructionsNums)) {
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
            .filter { it.isUnreachable(unreachableInstructionsNums) }
            .flatMap { collectAllUnreachableInstructionsFrom(it, unreachableInstructionsNums) }
        return listOf(instruction) + succUnreachable
    }
}


private fun Instruction.isUnreachable(alreadyUnreachable: Set<Int>): Boolean {
    val isFirstInstruction = num() == 0
    return !isFirstInstruction && allPred().all { alreadyUnreachable.contains(it.num()) }
}
