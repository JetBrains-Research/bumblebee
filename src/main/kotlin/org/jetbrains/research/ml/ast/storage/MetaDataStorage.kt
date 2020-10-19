package org.jetbrains.research.ml.ast.storage

import com.intellij.psi.PsiElement

interface MetaDataStorage {
    fun <T> setMetaData(psiTree: PsiElement, key: StorageKey<T>, metaData: T)

    fun <T> getMetaData(psiTree: PsiElement, key: StorageKey<T>): T?
}
