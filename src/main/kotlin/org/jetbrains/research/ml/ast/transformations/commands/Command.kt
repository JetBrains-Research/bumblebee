package org.jetbrains.research.ml.ast.transformations.commands

import com.intellij.psi.PsiElement
import java.util.concurrent.Callable


class Command<T>(val redo: Callable<T>, val undo: Callable<*>, val description: String)

// maybe there is a better way
abstract class CommandProvider<P, T> {
    protected abstract fun redo(input: P): Callable<T>
    protected abstract fun undo(input: P): Callable<*>

    fun getCommand(input: P, description: String) = Command(redo(input), undo(input), description)
}

/* Todo: add commands for:
- delete
- add
- replace
- rename
- addBefore
- addRangeBefore
- addRangeAfter
- deleteChildRange

 */


