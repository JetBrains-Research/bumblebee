# Anonymization

## Overview

This transformation renames most named entities in a program to have
automatically generated names.

## Naming scheme

Each named entity is renamed to have the form
`parentName + kindPrefix + kindIndex`, where:
- `parentName` is the name of the entity's parent after renaming. 
  The parent is usually the
  most nested scope owner that the entity's definition resides in.
  For example, the parent of a class method is the corresponding class.
  Global entities do not have a parent and, for them, `parentName` is
  considered to be empty.
  
- `kindPrefix` is one of the following, depending on what kind of entity
  is being renamed:

    | Prefix | Kind                    |
    | ------ | ----                    |
    | `f`    | Function                |
    | `v`    | Variable                |
    | `c`    | Class                   |
    | `p`    | Parameter               |
    | `m`    | Import alias ("module") |
    | `l`    | Lambda expression       |
 
  Note: lambda expressions do not have a name, thus cannot be renamed. However,
  they are assigned a "name" in case it needs to be used as an entity's
  `parentName`.
- `kindIndex` is a 1-based index of all entities of this entity's kind within
  its parent scope.
  
### Exceptions

- Any entities whose name starts with `__` (two underscores)
  are not renamed. This, in particular, preserves special method names
  (`__init__` etc.) and [name mangling](https://docs.python.org/3/reference/expressions.html#atom-identifiers).
- Any parameters of class methods that have one of the names `self` and `cls`
  are not renamed.
- If any `parentName` starts with two underscores, those are trimmed. This
  sometimes helps to avoid unwanted name mangling.

## Examples
```
var = 1
another_var = 2

def foo(param):
  inner = var
```
is transformed to:
```
v1 = 1
v2 = 2

def f1(f1p1):
  f1v1 = v1
```
