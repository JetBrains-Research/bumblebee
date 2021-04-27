package org.jetbrains.research.ml.ast.transformations

import com.intellij.psi.PsiElement

abstract class InversableTransformation : Transformation() {
    abstract fun inverseApply(psiTree: PsiElement)
}
