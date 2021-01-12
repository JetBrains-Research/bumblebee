# If Statement Redundant Lines Removal Transformation

## Overview

The goal of this transformation is to move to before or after the operator all redundant lines from `if` statement.

## Example

Before:
```python
s = input()

if s:
    a = 2
    b = a + a
    print('foo')
    x = 1
    y = x
    print(y)
else:
    a = 2
    b = a + a
    print('bar')
    x = 1
    y = x
    print(y)
```

After:
```python
s = input()

a = 2
b = a + a
if s:
    print('foo')
else:
    print('bar')
x = 1
y = x
print(y)
```


## Algorithm

1. Traverse the AST and find the node which represents the if statement

2. Calculate the length of the common suffix and prefix

3. Move before and after of the operator the common suffix and prefix 

4. Remove the common parts from the `if` operator

5. Validate the new `if` operator


## Exceptions

- If the common part is in the middle of the statements list the statement does not change,
because in this case, the length of the common suffix and prefix is zero.
An example of unchanged code:

```python
s = input()

if s:
    print('foo')
    a = 2
    b = a + a
    print('bar')
else:
    print('foo1')
    a = 2
    b = a + a
    print('bar1')
```

- The `PyStatment` comparison is done using the `textMatches` function, 
  which compares the text within these statements
