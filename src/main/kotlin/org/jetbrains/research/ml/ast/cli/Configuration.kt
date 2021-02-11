package org.jetbrains.research.ml.ast.cli

import com.charleskorn.kaml.Yaml.Companion.default
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.research.ml.ast.transformations.Transformation
import org.reflections.Reflections

internal object TransformationsStorage {
    private val allTransformations = getListOfAllTransformations()

    private fun getListOfAllTransformations(): List<Transformation> {
        val reflections = Reflections("org.jetbrains.research.ml.ast.transformations")
        val allObjects = reflections.getSubTypesOf(Transformation::class.java)
        return allObjects.mapNotNull { it.kotlin.objectInstance }
    }

    fun getTransformationByKey(key: String): Transformation? {
        val transformationsMap = allTransformations.map { it.key.toLowerCase() to it }.toMap()
        return transformationsMap[key.toLowerCase()]
    }
}

@Serializable
data class CliTransformation(
    val key: String,
    @SerialName("p")
    val probability: Double
) {
    fun getTransformationObject(): Transformation? = TransformationsStorage.getTransformationByKey(key)
}

@Serializable
data class Configuration(
    @SerialName("n transformations")
    val numTransformations: Int,
    val transformations: List<CliTransformation>
) {
    companion object {
        fun parseYamlConfig(config: String): Configuration = default.decodeFromString(serializer(), config.trimIndent())
    }
}
