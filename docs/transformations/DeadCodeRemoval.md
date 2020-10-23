# Dead Code Removal

## Overview

The goal of this transformation is to remove all unnecessary code which is never executed during any call of the function. 

## Example:

```python
def foo(x):
    if x >= 0:
        return x // 10
        print(x)
    return -x 
    print(-x)
```
Transforms to:
```python
def foo(x):
    if x >= 0:
        return x // 10
    return -x
```

## Algorithm:

### Using Control Flow Graph
1. Build [CFG](https://en.wikipedia.org/wiki/Control-flow_graph) for the program.
2. Traverse the graph in reverse order and search for **unreachable** vertices.
    The vertex is unreachable if: 
    * The vertex is not the first in the graph.
    * There is no incoming edges into this vertex.
3. Delete the unreachable vertex. Iterate for all outcoming vertices from this vertex and again search for **unreachable** vertices.

### Using Heuristics

* For `while`, `if` evaluate conditions and remove the statements where condition is `False`.


## Examples:

* Basic `return`
```python
def foo():
    print(1)
    return
    print("Unreachable")
```

* `raise` exceptions
```python
def foo(x):
    if x == 1:
        raise AssertionError()
        print("Unreachable")
    else:
        raise ArithmeticError()
    print("Unreachable")
```
* Loop `continue`, `break` statements
```python
def foo(x):
    for _ in range(x):
        if _ % 3 == 0:
            break
            print("Unreachable")
        print(1)
        continue
        print("Unreachable")
```
* Infinite `while` loop
```python
def foo():
    while True:
        print(1)
    print("Unreachable")
```

* Constant false `if` clause
```python
def foo():
    if 2 + 2 != 4:
        print("Unreachable")
```

* Constant false `while` loop
```python
def foo():
    while 2 + 2 != 4:
        print("Unreachable")
```