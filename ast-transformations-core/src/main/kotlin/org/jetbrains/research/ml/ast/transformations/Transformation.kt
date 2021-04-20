package org.jetbrains.research.ml.ast.transformations

import com.intellij.psi.PsiElement
import org.jetbrains.research.ml.ast.transformations.commands.CommandPerformer
import org.jetbrains.research.ml.ast.transformations.commands.ICommandPerformer

/**
 * The basic interface for AST transformations
 */
abstract class Transformation {

    abstract val key: String

    /**
     * Applying forward transformation.
     * @param [psiTree] - a hierarchy of PSI elements
     * @param [commandPerformer] - by default, a command storage that doesn't store any commands; pass the one that
     * stores commands to be able to undo them.
     */
    abstract fun forwardApply(psiTree: PsiElement, commandPerformer: ICommandPerformer = CommandPerformer(psiTree, false))
}
