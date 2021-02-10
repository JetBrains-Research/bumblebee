package org.jetbrains.research.ml.ast.util

fun <T> Iterable<T>.fold1(operation: (acc: T, T) -> T): T {
    return drop(1).fold(first(), operation)
}
