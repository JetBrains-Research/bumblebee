package org.jetbrains.research.ml.ast.storage

import com.intellij.psi.PsiElement

object InMemoryStorage : MetaDataStorage {
    private val storage = mutableMapOf<Pair<PsiElement, StorageKey<*>>, Any>()

    override fun <T> setMetaData(psiTree: PsiElement, key: StorageKey<T>, metaData: T) {
        storage[psiTree to key] = metaData!!
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getMetaData(psiTree: PsiElement, key: StorageKey<T>): T? {
        return storage[psiTree to key] as T?
    }
}
