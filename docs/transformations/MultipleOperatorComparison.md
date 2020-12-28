# Multiple Operator Comparison

## Overview

Turning multiple-operator comparisons into combinations of single-operator comparisons

```python
-1 <= x > y < z <= 1
```

Transforms to:

```python
-1 <= x and x > y and y < z and z <= 1
```

## Algorithm

Using the fact that we can represent the multiple-operator comparison expression using it's left associativity.

So we can recursively transform the left expression of multiple-operator comparison expression into conjunction of two expressions. 

