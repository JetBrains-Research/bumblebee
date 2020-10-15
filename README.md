[![Actions Status](https://github.com/nbirillo/ast-transformations/workflows/build/badge.svg)](https://github.com/nbirillo/ast-transformations/actions)


# AST-transformations

An IntelliJ-based IDE plugin for performing Python AST transformations using PSI elements

## Installation

Just clone the repo by `git clone https://github.com/nbirillo/ast-transformations.git` and run `./gradlew build shadowJar` to build a .zip distribution of the plugin. 
The .zip is located in `build/distributions/coding-assistant-1.0-SNAPSHOT.zip`. Then __install the plugin from disk__ into an IntelliJ-based IDE of your choice
(see [this guide](https://www.jetbrains.com/help/idea/managing-plugins.html#install_plugin_from_disk) for example). 

## Getting started

To run the plugin run `runIde` Gradle task provided by [gradle-intellij-plugin](https://github.com/JetBrains/gradle-intellij-plugin).

To add `git hook` for auto-formatting the project according to the code style guide before each commit 
run `./gradlew addKtlintFormatGitPreCommitHook`. As the result, the `.git` folder will contain the necessary hook.
