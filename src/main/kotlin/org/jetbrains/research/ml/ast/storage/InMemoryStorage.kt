package org.jetbrains.research.ml.ast.storage

import com.intellij.psi.PsiElement

object InMemoryStorage: MetaDataStorage {
    private val storage = mutableMapOf<Pair<PsiElement, String>, Any>()

    override fun <T> setMetaData(psiTree: PsiElement, key: String, metaData: T) {
        storage[psiTree to key] = metaData!!
    }

    override fun <T> getMetaData(psiTree: PsiElement, key: String): T? {
        return storage[psiTree to key] as? T?
    }
}
