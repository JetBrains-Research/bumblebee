package org.jetbrains.research.ml.ast.transformations.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import org.jetbrains.research.ml.ast.util.FileTestUtil
import org.jetbrains.research.ml.ast.util.ParametrizedBaseTest
import org.junit.Ignore
import java.io.File
import java.util.logging.Logger
import kotlin.reflect.KFunction

/**
 * We want to ensure that all tests classes for transformations have the required functionality
 */
interface IBaseTransformationsTest {
    fun assertCodeTransformation(
        inFile: File,
        outFile: File,
        transformation: (PsiElement, Boolean) -> Unit
    )
}

/**
 * We moved out common functions and variables to make classes easier for tests with SDK and without SDK
 */
interface IBaseTransformationsTestHelper {
    var codeStyleManager: CodeStyleManager

    fun assertCodeTransformation(
        inFile: File,
        outFile: File,
        transformation: (PsiElement, Boolean) -> Unit,
        fixture: CodeInsightTestFixture,
        project: Project?,
        toCheckFileStructure: Boolean = true,
        logger: Logger = Logger.getLogger(javaClass.name)
    )
}

@Ignore
open class BaseTransformationsTestHelper : IBaseTransformationsTestHelper {
    override lateinit var codeStyleManager: CodeStyleManager

    companion object {
        fun getInAndOutArray(
            cls: KFunction<ParametrizedBaseTest>,
            resourcesRootName: String,
        ): List<Array<File>> {
            val inAndOutFilesMap = FileTestUtil.getInAndOutFilesMap(
                ParametrizedBaseTest.getResourcesRootPath(
                    cls,
                    resourcesRootName
                )
            )
            return inAndOutFilesMap.entries.map { (inFile, outFile) -> arrayOf(inFile, outFile) }
        }
    }

    override fun assertCodeTransformation(
        inFile: File,
        outFile: File,
        transformation: (PsiElement, Boolean) -> Unit,
        fixture: CodeInsightTestFixture,
        project: Project?,
        toCheckFileStructure: Boolean,
        logger: Logger
    ) {
        logger.info("The current input file is: ${inFile.path}")
        logger.info("The current output file is: ${outFile.path}")
        val psiInFile = getPsiFile(inFile, fixture, project)
        val expectedPsiInFile = getPsiFile(outFile, fixture, project)
        val expectedSrc = expectedPsiInFile.text
        logger.info("The expected code is:\n$expectedSrc")
        ApplicationManager.getApplication().invokeAndWait {
            transformation(psiInFile, true)
            if (toCheckFileStructure) {
                PsiTestUtil.checkFileStructure(psiInFile)
            }
        }
        formatPsiFile(psiInFile, project)
        val actualSrc = psiInFile.text
        logger.info("The actual code is:\n$actualSrc")
        BasePlatformTestCase.assertEquals(expectedSrc, actualSrc)
    }

    private fun getPsiFile(
        file: File,
        fixture: CodeInsightTestFixture,
        project: Project?,
        toReformatFile: Boolean = true
    ): PsiFile {
        val psiFile = fixture.configureByFile(file.path)
        if (toReformatFile) {
            formatPsiFile(psiFile, project)
        }
        return psiFile
    }

    private fun formatPsiFile(psi: PsiElement, project: Project?) {
        WriteCommandAction.runWriteCommandAction(project) { // reformat the expected file
            codeStyleManager.reformat(psi)
        }
    }
}
