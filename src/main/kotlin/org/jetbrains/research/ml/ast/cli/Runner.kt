package org.jetbrains.research.ml.ast.cli

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationStarter
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.jetbrains.python.PythonFileType
import com.jetbrains.python.configuration.PyConfigurableInterpreterList
import com.jetbrains.python.sdk.*
import com.jetbrains.python.sdk.configuration.PyProjectVirtualEnvConfiguration
import com.jetbrains.python.statistics.modules
import com.xenomachina.argparser.ArgParser
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

    private fun getTmpProjectDir(): String {
        val path = "${System.getProperty("java.io.tmpdir")}/astTransformationsTmp"
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
            println("sdk: $sdk")
        }
        return sdk ?: error("Internal error: SDK for temp project was not created")
    }

    fun addPyFileToProject(
        project: Project,
        fileName: String = "main.py",
        fileContext: String = "print(\"Hello World\")"
    ) {
        val filePath = "${project.basePath}/$fileName"
        val file = File(filePath)
        file.createNewFile()
        file.writeText(fileContext)
    }

    override fun main(args: List<String>) {
        try {
            ArgParser(args.drop(1).toTypedArray()).parseInto(::TransformationsRunnerArgs).run {
                inputDir = Paths.get(input).toString()
                outputDir = Paths.get(output).toString().removeSuffix("/")
                yaml_config = Paths.get(yaml).toString()
            }

//            ApplicationManager.getApplication().invokeAndWait {
                project = ProjectUtil.openOrImport(getTmpProjectDir(), null, true)
                println("SDKS: ${PythonSdkUtil.getAllSdks()}")
                println("${System.getProperty("java.io.tmpdir")}/astTransformationsTmp")

                project?.let {

                    addPyFileToProject(it)


                    val myInterpreterList = PyConfigurableInterpreterList.getInstance(it)
                    val myProjectSdksModel = myInterpreterList.model

//                Just trying to somehow obtain sdk list automatically but with no success
//                var sdks: List<Sdk> = myProjectSdksModel.sdks.toList()
//                println("sdks: ${sdks.size}")
//                sdks = myInterpreterList.getAllPythonSdks(it)
//                println("sdks: ${sdks.size}")
//                sdks = PyConfigurableInterpreterList.getInstance(null).allPythonSdks
//                println("sdks: ${sdks.size}")
//                sdks = ProjectJdkTable.getInstance().allJdks.toList()
//                println("sdks: ${sdks.size}")

//            Todo: Somehow get SDK from python bin?? see PythonSdkType, PySdkProvider
//            "/usr/bin/python3"

                    val pySdkType = PythonSdkType.getInstance()
                    val baseSdk = myProjectSdksModel.createSdk(pySdkType, "/usr/bin/python3")
                    println(baseSdk)

                    val projectManager = ProjectRootManager.getInstance(it)
                    val sdk = createSdk(it, baseSdk)
                    val sdkConfigurer = SdkConfigurer(it, projectManager)
                    sdkConfigurer.setProjectSdk(sdk)
                    println("Project sdk: ${it.pythonSdk}")
                    it.modules.forEachIndexed { i, module ->
                        println("Module-$i sdk: ${module.pythonSdk}")
                    }

                    val fileFactory = PsiFileFactory.getInstance(it)
                    createFolder(outputDir)
                    val config = Configuration.parseYamlConfig(getContentFromFile(yaml_config))
                    // TODO: should we handle all nested folders and save the folders structure
                    val inputFiles = getFilesFormFolder(inputDir)
                    println("Num transformations: ${config.numTransformations}")
                    repeat(config.numTransformations) { num ->
                        val currentPath = "$outputDir/${num}_transformation"
                        createFolder(currentPath)
                        val transformationsToApply = filterTransformations(config)
                        println("Transformations to apply: ${transformationsToApply.size}")
                        inputFiles.forEach { file ->
                            val psi = file.createPsiFile(fileFactory)
                            println(("Psi project: ${psi.project.name}"))
                            println("Psi name: ${psi.name}")
                            psi.applyTransformations(transformationsToApply)
                            println("Psi name 2: ${psi.name}")
                            println("$currentPath/${file.name}")
                            createFile("$currentPath/${file.name}", psi.text)
                        }
                    }
                } ?: error("Internal error: the temp project was not created")
//            }
        } catch (ex: Exception) {
            logger.error(ex)
        } finally {
            exitProcess(0)
        }
    }
}
