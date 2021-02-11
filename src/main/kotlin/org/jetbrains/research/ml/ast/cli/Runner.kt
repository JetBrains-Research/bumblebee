package org.jetbrains.research.ml.ast.cli

import com.intellij.openapi.application.ApplicationStarter
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.xenomachina.argparser.ArgParser
import org.jetbrains.research.ml.ast.transformations.Transformation
import org.jetbrains.research.ml.ast.util.getContentFromFile
import java.nio.file.Paths
import kotlin.random.Random.Default.nextDouble
import kotlin.system.exitProcess

object Runner : ApplicationStarter {
    private const val YAML_CONFIG = "config.yaml"

    private lateinit var inputDir: String

    private var project: Project? = null
    private val logger = Logger.getInstance(this::class.java)

    override fun getCommandName(): String = "python-transformations"

    class TransformationsRunnerArgs(parser: ArgParser) {
        val input by parser.storing(
            "-i",
            "--path",
            help = "Input directory with python files"
        )
    }

    override fun main(args: Array<out String>) {
        try {
            ArgParser(args.drop(1).toTypedArray()).parseInto(::TransformationsRunnerArgs).run {
                inputDir = Paths.get(input).toString()
            }

            // TODO: create tmp project

            val config = Configuration.parseYamlConfig(getContentFromFile(YAML_CONFIG))
            repeat(config.numTransformations) { num ->
                // TODO: create output folder

                val transformationsToApply: MutableList<Transformation> = ArrayList()
                config.transformations.forEach { transfromation ->
                    val p = nextDouble(0.0, 1.0)
                    if (transfromation.probability <= p) {
                        transfromation.getTransformationObject()?.let {
                            transformationsToApply.add(it)
                        } ?: logger.error("Incorrect transformation key ${transfromation.key}")
                    }
                }
                // TODO: apply transformations
            }
        } catch (ex: Exception) {
            logger.error(ex)
        } finally {
            exitProcess(0)
        }
    }
}
