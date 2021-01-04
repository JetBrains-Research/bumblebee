# Comparison Unification

This transformation replaces comparison operators `<`, `<=` into `>`, `>=`.

## Algorithm

Find all actual AST nodes, invert children and replace binary operator.

## Example
Before:

```python
if x < y and y <= 4:
    pass
```

After:

```python
if x > y and y >= 4:
    pass
```