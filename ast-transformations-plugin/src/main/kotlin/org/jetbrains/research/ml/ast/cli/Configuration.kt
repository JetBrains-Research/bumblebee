package org.jetbrains.research.ml.ast.cli

import com.charleskorn.kaml.Yaml.Companion.default
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.research.ml.ast.transformations.Transformation
import org.reflections.Reflections

internal object TransformationsStorage {
    // TODO: use Reflekt compiler plugin
    private val allTransformationsMap = getListOfAllTransformations().associateBy { it.key.toLowerCase() }

    private fun getListOfAllTransformations(): List<Transformation> {
        val reflections = Reflections("org.jetbrains.research.ml.ast.transformations")
        val allObjects = reflections.getSubTypesOf(Transformation::class.java)
        return allObjects.mapNotNull { it.kotlin.objectInstance }
    }

    fun getTransformationByKey(key: String): Transformation? = allTransformationsMap[key.toLowerCase()]
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
    @SerialName("n apply")
    val nApply: Int,
    val transformations: List<CliTransformation>
) {
    companion object {
        fun parseYamlConfig(config: String): Configuration = default.decodeFromString(serializer(), config.trimIndent())
    }
}
