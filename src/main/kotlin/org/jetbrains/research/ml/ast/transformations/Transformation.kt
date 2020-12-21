package org.jetbrains.research.ml.ast.transformations

import com.intellij.psi.PsiElement

/**
 * The basic interface for AST transformations
 */
abstract class Transformation {

    abstract val key: String

    /**
     * Applying forward transformation.
     * @param [psiTree] - a hierarchy of PSI elements
     * @param [commandsStorage] - if not null, all commands, performed on [psiTree], will be saved and available to undo.
     */
    abstract fun forwardApply(psiTree: PsiElement, commandsStorage: PerformedCommandStorage? = null)

    /**
     * Applying backward transformation.
     * @param [commandsStorage] - should store all commands performed during [forwardApply]
     */
    fun backwardApply(commandsStorage: PerformedCommandStorage): PsiElement {
//      Todo: maybe we need to undo only the commands performed in the current transformation?
//       commandStorage may store other command too, not only belonged to this transformation
        return commandsStorage.undoPerformedCommands()
    }
}
