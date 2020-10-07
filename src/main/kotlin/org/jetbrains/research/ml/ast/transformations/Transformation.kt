package org.jetbrains.research.ml.ast.transformations

import com.intellij.psi.PsiElement

/**
 * The basic interface for AST transformations
 */
interface Transformation {

    val metadataKey: String

    /**
     * Applying forward transformation.
     * @param [psiTree] - a hierarchy of PSI elements
     * @param [toStoreMetadata] - flag that indicates necessity of metadata storing.
     */
    fun apply(psiTree: PsiElement, toStoreMetadata: Boolean)

    /**
    * Applying reverse transformation.
    * @param [psiTree] - a hierarchy of PSI elements
    */
    fun inverseApply(psiTree: PsiElement)
}