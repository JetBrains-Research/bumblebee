package org.jetbrains.research.ml.ast.util

import java.io.File

fun getContentFromFile(filePath: String): String = File(filePath).readText(Charsets.UTF_8)
