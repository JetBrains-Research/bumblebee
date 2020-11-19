# Multiple Target Assignment

## Overview

Simplifies Python [assignment statement](https://python.readthedocs.io/en/stable/reference/simple_stmts.html#assignment-statements).

## Example

Before:
```python
class A:
    pass
a = b = c = A() 
```

After:
```python
class A:
    pass
a = A()
b = a
c = b
```

The transformation has invariant `id(a) == id(b) == id(c)`.


## Algorithm

1. Traverse the AST and find the node which represents the assignment statement

2. Replace this node with the sequence of simple assignments.