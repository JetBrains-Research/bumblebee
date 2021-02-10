package org.jetbrains.research.ml.ast.cli

import kotlinx.serialization.Serializable
import org.yaml.snakeyaml.Yaml

@Serializable
data class Configuration(
    val numTransformations: Int
)

//fun main() {
//    val input = Configuration(5)
//    val a = Yaml.default.encodeToString(Configuration.serializer(), input)
//}
