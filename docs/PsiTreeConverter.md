# PsiTreeConverter description
 
## Table of Contents:
 
- [Short description](#short-description)
- [GumTree labels](#gumtree-labels)
 
## Short description
 
This document contains the test cases description for the converter from
[PSI](https://jetbrains.org/intellij/sdk/docs/basics/architectural_overview/psi.html) to GumTree
[tree](https://github.com/GumTreeDiff/gumtree/blob/develop/core/src/main/java/com/github/gumtreediff/tree/ITree.java).
 
Each node from the GumTree tree stores the following values:
- typeLabel - according to `psiTree.node.elementType.toString()`
- label - according to the `PsiElement.label` function
- id - according to the numbering (PostOrder or PreOrder)
 
We store all the nodes from the PSI tree that we can visit by DFS traversal from the root.
 
## GumTree labels
 
The rules for generating the labels for the PSI node:
1. If the node is a leaf node, we store the text. For example `PyTargetExpression`, `PyPassStatement`,
`PyReferenceExpression`, and so on
2. If it is not a leaf node, there are several cases to consider. We can not store the text, because it contains the text from the
current vertex and from all the children. For example, consider the following code:
```
for i in range(100):
  pass
```
 
The PSI is:
 
```
PyFile:Dummy.py
 PyForStatement
   PyForPart
     PyTargetExpression: i
     PyCallExpression: range
       PyReferenceExpression: range
       PyArgumentList
         PyNumericLiteralExpression
     PyStatementList
       PyPassStatement
 
```
 
The node `PyForStatement` has the text:
 
```
for i in range(100):
  pass
```
 
But we don't want to store a big code snapshot in the GumTree tree.
 
So in this case, we should consider the type of the current node.
 
Since all code elements are inherited from `PyBaseElementImpl<*>`, we need to consider special cases when we want
to store not a name (because it is `null`), but something else that characterizes this vertex.
 
Consider all used cases in the `PsiElement.label` function:
 
- `PyBinaryExpression` has the name `null`, but we need to distinguish between operations depending on the action they
perform. For example, the nodes with expressions `a + b` and `a - b` have the same PSI structure,
but the operators are different. In this case we store `operator` or `specialMethodName` for the operator if it exists.
For example, in the expression `a + b` we have `PyBinaryExpression` with the operator's `specialMethodName` `__add__`,
but the operator is `Py:PLUS`.
 
- `PyPrefixExpression` has the name `null`, but we also need to distinguish between operations depending on the action
they perform. An example of this expression is `-1` or `not True`. It does not have the `specialMethodName` field,
but it has the `operator` field.
 
- `PyAugAssignmentStatement` supports the operators like `+=`, `-=`, and so on. We store `operation.text` which lists
the action like `+=` or `-=` if it exists.
 
- `PyFormattedStringElement` supports [f-strings](https://docs.python.org/3/reference/lexical_analysis.html#f-strings).
We did not find a way to separate text content from the variables, and so we store full content like `f"text {1}"`
We have a `todo` about this.
 
- `PyImportElement` has a name, but it stores the name of the library twice -- in this node and in a child.
So we use an empty name for this node and store the name only in the child node.
 
- `PyYieldExpression` can have the prefix `from` if you use a collection: `yield from [1, 2, 3]` and `yield 3`.
However, we can not get this information from the PSI node (only from the text). We need to distinguish these cases in the
GumTree and so we store the label `from` if it is `yield` for a collection.
 
In most cases, we can use the name of the node or an empty label.
For example, almost all elements that inherit from `PyBaseElementImpl<*>` have a defining (or empty) name.
Consider the following examples:
- `PyWithItem` or `PyArgumentList` has an empty name because it has nested items in the tree.
- `PyReferenceExpression` has a reference name, for example `with open('file_1', 'r')` has `PyReferenceExpression`
 with the name `open`. We need to store it to distinguish between different called functions.
We are not interested in the unconsidered vertices, since for them it is usually enough just to know the information
about their type. In this case, we are sure that it is not `PyBaseElementImpl<*>`.
For example, it is true for the type `FILE`.
 
To create this list of cases we considered all types of the Py PSI items and decided on what additional information we need.
 
### Unsupported cases
 
**Note:** we don't support the following cases:
- `async` keyword;
- type annotations;
 
This means that if these cases occur in the processed code, the converter will work, but the tree can be the same for the following cases:
- `a: int = 1`
- `a: float = 1`
 
We are going to support these cases in the future.
