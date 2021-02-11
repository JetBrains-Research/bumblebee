package org.jetbrains.research.ml.ast.cli

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationStarter
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.jetbrains.python.PythonFileType
import com.xenomachina.argparser.ArgParser
import org.jetbrains.research.ml.ast.transformations.Transformation
import org.jetbrains.research.ml.ast.util.createFile
import org.jetbrains.research.ml.ast.util.createFolder
import org.jetbrains.research.ml.ast.util.getContentFromFile
import org.jetbrains.research.ml.ast.util.getFilesFormFolder
import java.io.File
import java.nio.file.Paths
import kotlin.random.Random.Default.nextDouble
import kotlin.system.exitProcess

// TODO: test it with different transformations sets
object Runner : ApplicationStarter {
    private lateinit var inputDir: String
    private lateinit var outputDir: String
    private lateinit var yaml_config: String

    private var project: Project? = null
    private val logger = Logger.getInstance(this::class.java)

    override fun getCommandName(): String = "python-transformations"

    class TransformationsRunnerArgs(parser: ArgParser) {
        val input by parser.storing(
            "-i",
            "--input_path",
            help = "Input directory with python files"
        )

        val output by parser.storing(
            "-o",
            "--output_path",
            help = "Output directory"
        )

        val yaml by parser.storing(
            "-y",
            "--yaml_path",
            help = "YAML config path"
        )
    }

    private fun getTmpProjectDir(): String {
        val path = "${System.getProperty("java.io.tmpdir")}/tmpProject"
        createFolder(path)
        return path
    }

    // Filter transformations according to the probability
    private fun filterTransformations(config: Configuration): List<Transformation> {
        val transformationsToApply: MutableList<Transformation> = ArrayList()
        config.transformations.forEach { transformation ->
            val p = nextDouble(0.0, 1.0)
            if (transformation.probability >= p) {
                transformation.getTransformationObject()?.let {
                    transformationsToApply.add(it)
                } ?: logger.error("Incorrect transformation key ${transformation.key}")
            }
        }
        return transformationsToApply
    }

    private fun File.createPsiFile(fileFactory: PsiFileFactory): PsiFile {
        return ApplicationManager.getApplication().runWriteAction<PsiFile> {
            val fileName = "dummy." + PythonFileType.INSTANCE.defaultExtension
            val fileType = PythonFileType.INSTANCE
            fileFactory.createFileFromText(fileName, fileType, getContentFromFile(this.absolutePath))
        }
    }

    private fun PsiFile.applyTransformations(transformations: List<Transformation>) {
        ApplicationManager.getApplication().invokeAndWait {
            ApplicationManager.getApplication().runWriteAction {
                transformations.forEach {
                    it.forwardApply(this)
                }
            }
        }
    }

    override fun main(args: Array<out String>) {
        try {
            ArgParser(args.drop(1).toTypedArray()).parseInto(::TransformationsRunnerArgs).run {
                inputDir = Paths.get(input).toString()
                outputDir = Paths.get(output).toString().removeSuffix("/")
                yaml_config = Paths.get(yaml).toString()
            }
            project = ProjectUtil.openOrImport(getTmpProjectDir(), null, true)
            project?.let {
                val fileFactory = PsiFileFactory.getInstance(project)
                createFolder(outputDir)
                val config = Configuration.parseYamlConfig(getContentFromFile(yaml_config))
                // TODO: should we handle all nested folders and save the folders structure
                val inputFiles = getFilesFormFolder(inputDir)
                repeat(config.numTransformations) { num ->
                    val currentPath = "$outputDir/${num}_transformation"
                    createFolder(currentPath)
                    val transformationsToApply = filterTransformations(config)
                    inputFiles.forEach {
                        val psi = it.createPsiFile(fileFactory)
                        psi.applyTransformations(transformationsToApply)
                        println("$currentPath/${it.name}")
                        createFile("$currentPath/${it.name}", psi.text)
                    }
                }
            } ?: error("Internal error: the temp project was not created")
        } catch (ex: Exception) {
            logger.error(ex)
        } finally {
            exitProcess(0)
        }
    }
}
