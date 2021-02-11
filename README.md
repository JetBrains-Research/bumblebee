[![JetBrains Research](https://jb.gg/badges/research.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Actions Status](https://github.com/nbirillo/ast-transformations/workflows/build/badge.svg)](https://github.com/nbirillo/ast-transformations/actions)


# AST-transformations

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
- [x] [Expression Unification](./docs/transformations/ExpressionUnification.md)
- [x] [If Statement Redundant Lines Removal](./docs/transformations/IfRedundantLinesRemovalTransformation.md)
- [x] [Multiple Operator Comparison](./docs/transformations/MultipleOperatorComparison.md)
- [x] [Multiple Target Assignment](./docs/transformations/MultipleTargetAssignmentTransformation.md)
- [x] [Outer Not Elimination](./docs/transformations/OuterNotElimination.md)

## Installation

Just clone the repo by `git clone https://github.com/JetBrains-Research/ast-transformations`.


## Getting started as a tool

Run the command `./gradlew :cli -Pinput=<Input directory with python files> -Poutput=<Output directory> -Pyaml=<YAML config path>`.
The YAML configuration file example can be found [here](./config.yaml).
Set of the transformations is applied `n transformations` times. Each round for each transformation the random  
number between `0.0` and `1.0` is generated. If this value less than `p` for the current transformation,
the transformation will be applied. The result for each round is stored in the separated folder.


## Getting started as a library

Import the plugin in your plugin:

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
2. Backward transformations
3. Find edits between two PSIs and apply them to get second PSI from the first PSI. 
   To find edits we use [GumTree](https://github.com/GumTreeDiff/gumtree) library.
   
All usage examples can be found in the [test](./src/test/kotlin/org/jetbrains/research/ml/ast) folder.
