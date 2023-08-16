[![JetBrains Research](https://jb.gg/badges/research.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Build](https://github.com/JetBrains-Research/bumblebee/actions/workflows/build.yml/badge.svg)](https://github.com/JetBrains-Research/bumblebee/actions/workflows/build.yml)


# Bumblebee

An IntelliJ-based IDE plugin for performing Python AST transformations using PSI elements. 
You can use this plugin as a library or as a tool to work with Python PSI. 
The differences between these options can be found below.

## Available transformations

- [x] [Anonymization](./docs/transformations/Anonymization.md)
- [x] [Augmented Assignment](./docs/transformations/AugmentedAssignment.md)
- [x] [Comments Removal](./docs/transformations/CommentsRemoval.md)
- [x] [Comparison Unification](./docs/transformations/ComparisonUnification.md)
- [x] [Constant folding](./docs/transformations/ConstantFolding.md)
- [x] [Dead Code Removal](./docs/transformations/DeadCodeRemoval.md)
- [x] [EmptyLinesRemoval](./docs/transformations/EmptyLinesRemoval.md)
- [x] [Expression Unification](./docs/transformations/ExpressionUnification.md)
- [x] [If Statement Redundant Lines Removal](./docs/transformations/IfRedundantLinesRemovalTransformation.md)
- [x] [Multiple Operator Comparison](./docs/transformations/MultipleOperatorComparison.md)
- [x] [Multiple Target Assignment](./docs/transformations/MultipleTargetAssignmentTransformation.md)
- [x] [Outer Not Elimination](./docs/transformations/OuterNotElimination.md)

## Installation

Just clone the repo by `git clone https://github.com/JetBrains-Research/ast-transformations`.


## Getting started as a tool

Run the command `./gradlew :ast-transformations-plugin:cli -Pinput=<Input directory with python files> -Poutput=<Output directory>`.
All transformations will be applied.

## Getting started as a library

You can use this plugin as a library in your plugin by importing it in `settings.gradle.kts` and `build.gradle.kts files`:

1. File `settings.gradle.kts`:

```kotlin
sourceControl {
    gitRepository(URI.create("https://github.com/JetBrains-Research/ast-transformations.git")) {
        producesModule("org.jetbrains.research.ml.ast.transformations:ast-transformations")
    }
}
```

2. File `build.gradle.kts`:

```kotlin
implementation("org.jetbrains.research.ml.ast.transformations:ast-transformations") {
    version {
        branch = "master"
    }
}
```

The available features:

1. Forward transformations
2. Backward transformations (unstable)
   
All usage examples can be found in the [test](./src/test/kotlin/org/jetbrains/research/ml/ast) folder.
