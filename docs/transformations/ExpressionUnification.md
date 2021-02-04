# Expression Unification

## Overview

This transformation standardizes binary expressions with commutative integer operands.

To standardize the binary expression we can swap operands using special rule.

## Restrictions

We need to transform the expression saving its meaning. So we are using only integer operands and commutative operators.

* operands are only integer
* operators are only [`*`, `+`, `|`, `&`, `^`]

## Algorithm

Recursively unify operands of binary expression.

__Base case.__
Binary expression in the form of `lhs (*+|&^) rhs` swap `lhs` and `rhs` if `lhs < rhs` lexicographically.

Then after these binary expression transformations we need to correct the expression associativity. The swap operations
can ruin the left operator associativity, so we have to collect all incorrect associativity vertices and recreate the
subtree with left associativity.

## Examples

Before:

```python
3 * x * 2 * 1
```

After:

```python
1 * 2 * 3 * x
```

---

Before:

```python
'h' + (4 + 9 * 1)
```

After:

```python
'h' + (1 * 9 + 4)
```

---

Before (`a`, `b`, `c` can be inferred as integer):

```python
c + a + b
```

After:

```python
a + b + c
```