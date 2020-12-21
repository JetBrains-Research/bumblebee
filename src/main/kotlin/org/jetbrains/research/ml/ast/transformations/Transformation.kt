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
     * @param [toStoreMetadata] - flag that indicates necessity of metadata storing.
     */
    abstract fun apply(psiTree: PsiElement, metaDataStorage: MetaDataStorage? = null)

    /**
     * Applying reverse transformation.
     */
    fun inverseApply(metaDataStorage: MetaDataStorage) {
        metaDataStorage.undoCommands()
    }
}
