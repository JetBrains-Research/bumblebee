package org.jetbrains.research.ml.ast.cli

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationStarter
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.jetbrains.python.PythonFileType
import com.jetbrains.python.configuration.PyConfigurableInterpreterList
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.configuration.PyProjectVirtualEnvConfiguration
import com.xenomachina.argparser.ArgParser
import org.jetbrains.annotations.NotNull
import org.jetbrains.research.ml.ast.transformations.Transformation
import org.jetbrains.research.ml.ast.util.*
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

    private fun getTmpProjectDir(toCreateFolder: Boolean = true): String {
        val path = "${System.getProperty("java.io.tmpdir").removeSuffix("/")}/astTransformationsTmp"
        if (toCreateFolder) {
            createFolder(path)
        }
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

    private fun File.createPsiFile(psiManager: PsiManager): PsiFile {
        return ApplicationManager.getApplication().runWriteAction<PsiFile> {
            val basePath = getTmpProjectDir(toCreateFolder = false)
            val fileName = "dummy." + PythonFileType.INSTANCE.defaultExtension
            val content = getContentFromFile(this.absolutePath)
            val file = addPyFileToProject(basePath, fileName, content)
            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
            psiManager.findFile(virtualFile!!)
        }
    }

    private fun PsiFile.applyTransformations(transformations: List<Transformation>) {
        ApplicationManager.getApplication().invokeAndWait {
            ApplicationManager.getApplication().runWriteAction {
                transformations.forEach {
                    println(it.key)
                    it.forwardApply(this)
                }
            }
        }
    }

    private fun createSdk(project: Project, baseSdk: Sdk): Sdk {
        var sdk: Sdk? = null
        ApplicationManager.getApplication().invokeAndWait {
            sdk = PyProjectVirtualEnvConfiguration.createVirtualEnvSynchronously(
                baseSdk = baseSdk,
                existingSdks = listOf(baseSdk),
                venvRoot = getTmpProjectDir(),
                projectBasePath = project.basePath,
                project = project,
                module = null
            )
        }
        return sdk ?: error("Internal error: SDK for temp project was not created")
    }

    private fun addPyFileToProject(
        projectPath: String,
        fileName: String,
        fileContext: String = ""
    ): File {
        val filePath = "$projectPath/$fileName"
        val file = File(filePath)
        file.createNewFile()
        file.writeText(fileContext)
        return file
    }

    private fun createBaseSdk(project: Project): Sdk {
        val myInterpreterList = PyConfigurableInterpreterList.getInstance(project)
        val myProjectSdksModel = myInterpreterList.model
        val pySdkType = PythonSdkType.getInstance()
        return myProjectSdksModel.createSdk(pySdkType, "/usr/bin/python3")
    }

    override fun main(args: List<String>) {
        try {
            ArgParser(args.drop(1).toTypedArray()).parseInto(::TransformationsRunnerArgs).run {
                inputDir = Paths.get(input).toString()
                outputDir = Paths.get(output).toString().removeSuffix("/")
                yaml_config = Paths.get(yaml).toString()
            }

            project = ProjectUtil.openOrImport(getTmpProjectDir(), null, true)

            project?.let {
                val baseSdk = createBaseSdk(it)

                val projectManager = ProjectRootManager.getInstance(it)
                val sdk = createSdk(it, baseSdk)
                val sdkConfigurer = SdkConfigurer(it, projectManager)
                sdkConfigurer.setProjectSdk(sdk)

                val psiManager = PsiManager.getInstance(it)
                createFolder(outputDir)
                val config = Configuration.parseYamlConfig(getContentFromFile(yaml_config))
                // TODO: should we handle all nested folders and save the folders structure
                val inputFiles = getFilesFormFolder(inputDir)
                repeat(config.numTransformations) { num ->
                    val currentPath = "$outputDir/${num}_transformation"
                    createFolder(currentPath)
                    val transformationsToApply = filterTransformations(config)
                    inputFiles.forEach { file ->
                        val psi = file.createPsiFile(psiManager)
                        psi.applyTransformations(transformationsToApply)
                        createFile("$currentPath/${file.name}", psi.text)
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
