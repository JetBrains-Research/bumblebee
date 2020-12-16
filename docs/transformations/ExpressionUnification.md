# Expression Unification

## Overview

This transformation unifies binary expressions with commutative integer operands.

## Restrictions

* operands are only integer
* operators are only ["*", "+", "|", "&", "^"]

## Examples
* 
Before:
```python
3 * x * 2 * 1
```
After:
```python
1 * 2 * 3 * x
```

* 

Before:
```python
'h' + (4 + 9 * 1)
```
After:
```python
'h' + (1 * 9 + 4)
```