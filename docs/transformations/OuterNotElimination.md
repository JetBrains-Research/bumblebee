# Outer Not Elimination

## Overview

This transformation applies [De Morgan's laws](https://en.wikipedia.org/wiki/De_Morgan%27s_laws) to propagate the `not`
operator.

## Algorithm

Find the prefix `not` node with conjunction/disjunction inner binary expression. Propagate the not using the De Morgan's
laws. Repeat the process with the left and right binary expression operands.

## Example

Before:

```python
not (x and (y or z))
```

After:

```python
(not x or (not y and not z))
```

