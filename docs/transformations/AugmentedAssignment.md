# Augmented Assignment Transformation

## Overview

Unfold the augmented assignment statement into standard binary operation.

Supported augmented operations:

* `+=`  &#8594; `+`
* `-=`  &#8594; `-`
* `*=`  &#8594; `*`
* `@=`  &#8594; `@`
* `/=`  &#8594; `/`
* `//=` &#8594; `//`
* `%=`  &#8594; `%`
* `**=` &#8594; `**`
* `>>=` &#8594; `>>`
* `<<=` &#8594; `<<`
* `&=`  &#8594; `&`
* `^=`  &#8594; `^`
* `|=`  &#8594; `|`

To save the order of operations curly brackets are placed.
If the augmented operation value contains one or more binary operation we have to surround the expression with curly brackets.   

## Example:
```python
x += 3
x *= 2 + 2
```
```python
x = x + 3
x = x * (2 + 2)
```
