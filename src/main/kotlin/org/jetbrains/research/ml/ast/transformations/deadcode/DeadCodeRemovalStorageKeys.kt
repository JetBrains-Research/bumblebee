package org.jetbrains.research.ml.ast.transformations.deadcode

import org.jetbrains.research.ml.ast.storage.StorageKey

internal object DeadCodeRemovalStorageKeys {
    val NODE = StorageKey<List<String>>("Node")
}
