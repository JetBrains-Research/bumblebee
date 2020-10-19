package org.jetbrains.research.ml.ast.storage

import com.intellij.psi.PsiElement

interface MetaDataStorage {
    fun <T> setMetaData(psiTree: PsiElement, key: String, metaData: T)

    fun <T> getMetaData(psiTree: PsiElement, key: String): T?
}
