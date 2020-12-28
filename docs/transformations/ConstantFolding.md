# Constant folding

### Table of contents

- [Overview](#overview)
- [Working cases](#working-cases)
- [Algorithm](#algorithm)

### Overview

This transformation aims to evaluate as much numerical/boolean constants
as possible.

### Working cases

#### Fully constant integral/boolean expressions in any context

For example:
- `1 + 2 * 3 - 8 // 3 + 7 ^ 3 | 11` is replaced with `15`
- `True and 1 + 1 == 2` is replaced with `True`

*Note:* The power operator `**` is special in that the size of its result
can be much larger than the size of its operands. Because of this,
`**` is only evaluated when both its operands and its result fit into
the JVM Long type (64-bits signed integer).

#### Maximal (by inclusion) subexpressions of non-constant expressions

For example:
- `1 + 2 + x` is replaced with `3 + x`

#### Partially evaluating boolean `and` and `or` operators

For example:
- `[x, y] or 42` is replaced with `[x, y]`, since the left part is
  truthy regardless of what specific values `x` and `y` have.

#### Partially evaluating commutative operators

Arguments of known commutative operators are partially evaluated
regardless of the order of arguments given. All of the constant arguments do
not have to form a distinct subtree in this case.

For example:
- `x + 1 + 2` is replaced with `x + 3` when it is possible to deduce that
  `x` is of type `int` (to ensure the commutativity of `+`)

#### Concatenating strings and lists

For example:
- `"Hello, " + "world!"` is replaced with `"Hello, world!"`
- `[x] + [y]` is replaced with `[x, y]`

### Algorithm

The transformation works on any expression by recursively simplifying all
of its subexpressions and then evaluating the top-level node using the results.
