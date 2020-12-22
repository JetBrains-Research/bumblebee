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
}
