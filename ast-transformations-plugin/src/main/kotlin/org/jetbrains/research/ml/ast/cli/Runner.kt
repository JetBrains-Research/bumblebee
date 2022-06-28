package org.jetbrains.research.ml.ast.cli

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationStarter
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.jetbrains.python.PythonFileType
import com.xenomachina.argparser.ArgParser
import org.jetbrains.research.ml.ast.transformations.Transformation
import org.jetbrains.research.ml.ast.util.*
import org.jetbrains.research.ml.ast.util.sdk.setSdkToProject
import java.io.File
import java.nio.file.Paths
import kotlin.system.exitProcess

object Runner : ApplicationStarter {
    private lateinit var inputDir: String
    private lateinit var outputDir: String

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
                    it.forwardApply(this)
                }
            }
        }
    }

    override fun main(args: List<String>) {
        try {
            ArgParser(args.drop(1).toTypedArray()).parseInto(::TransformationsRunnerArgs).run {
                inputDir = Paths.get(input).toString()
                outputDir = Paths.get(output).toString().removeSuffix("/")
            }

            project = ProjectUtil.openOrImport(getTmpProjectDir(), null, true)
            project?.let { it ->
                setSdkToProject(it, getTmpProjectDir())
                val psiManager = PsiManager.getInstance(it)
                createFolder(outputDir)
                // TODO: should we handle all nested folders and save the folders structure
                val inputFiles = getFilesFormFolder(inputDir)
                createFolder(outputDir)
                inputFiles.forEach { file ->
                    val psi = file.createPsiFile(psiManager)
                    psi.applyTransformations(TransformationsStorage.getListOfAllTransformations())
                    createFile("$outputDir/${file.name}", psi.text)
                }
            } ?: error("Internal error: the temp project was not created")
        } catch (ex: Exception) {
            logger.error(ex)
        } finally {
            exitProcess(0)
        }
    }
}
