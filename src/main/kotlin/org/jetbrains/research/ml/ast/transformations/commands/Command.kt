package org.jetbrains.research.ml.ast.transformations.commands

import java.util.concurrent.Callable


class Command<T>(val redo: Callable<T>, val undo: Callable<*>, val description: String)

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


