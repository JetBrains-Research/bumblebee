# Comments Removal

## Overview

This transformation deletes all comments in the code. 

## Algorithm

Find all comments and delete them. The supported comments type:

[x] Single line comment (starts with the hash character `#`)

[x] Multi lines comment with using `'''...'''`

[x] Multi lines comment with using `"""..."""`

## Examples

<details><summary>Single line comment example</summary>
<p>

Code before transformation:

```
# One line comment
def foo():
    # One line comment
    pass
```

Code after transformation:

```
def foo():
    pass
```

</p>
</details>


<details><summary>Multi lines comment with using `'''...'''` example</summary>
<p>

Code before transformation:

```
'''
This is a comment
written in
more than just one line
'''
def foo():
    '''
    This is a comment
    written in
    more than just one line
    '''
    pass
```

Code after transformation:

```
def foo():
    pass
```

</p>
</details>

<details><summary>Multi lines comment with using `"""..."""` example</summary>
<p>

Code before transformation:

```
"""
This is a comment
written in
more than just one line
"""
def foo():
    """
    This is a comment
    written in
    more than just one line
    """
    pass
```

Code after transformation:

```
def foo():
    pass
```

</p>
</details>

