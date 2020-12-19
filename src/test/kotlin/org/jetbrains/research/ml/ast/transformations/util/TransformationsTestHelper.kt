package org.jetbrains.research.ml.ast.transformations.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.research.ml.ast.util.FileTestUtil
import org.jetbrains.research.ml.ast.util.ParametrizedBaseTest
import org.jetbrains.research.ml.ast.util.PsiFileHandler
import java.io.File
import java.util.logging.Logger
import kotlin.reflect.KFunction

/**
 * We want to ensure that all tests classes for transformations have the required functionality
 */
interface ITransformationsTest {
    fun assertCodeTransformation(
        inFile: File,
        outFile: File,
        transformation: (PsiElement, Boolean) -> Unit
    )
}

/**
 * We moved out common functions to make classes easier for tests with SDK and without SDK
 */
object TransformationsTestHelper {
    private val logger = Logger.getLogger(javaClass.name)

    fun getInAndOutArray(cls: KFunction<ParametrizedBaseTest>, resourcesRootName: String): List<Array<File>> {
        val inAndOutFilesMap = FileTestUtil.getInAndOutFilesMap(
            ParametrizedBaseTest.getResourcesRootPath(cls, resourcesRootName)
        )
        return inAndOutFilesMap.entries.map { (inFile, outFile) -> arrayOf(inFile, outFile) }
    }

    fun assertCodeTransformation(
        inFile: File,
        outFile: File,
        transformation: (PsiElement, Boolean) -> Unit,
        fileHandler: PsiFileHandler,
        toCheckFileStructure: Boolean
    ) {
        logger.info("The current input file is: ${inFile.path}")
        logger.info("The current output file is: ${outFile.path}")
        val psiInFile = fileHandler.getPsiFile(inFile)
        val expectedPsiInFile = fileHandler.getPsiFile(outFile)
        val expectedSrc = expectedPsiInFile.text
        logger.info("The expected code is:\n$expectedSrc")
        ApplicationManager.getApplication().invokeAndWait {
            transformation(psiInFile, true)
            if (toCheckFileStructure) {
                PsiTestUtil.checkFileStructure(psiInFile)
            }
        }
        fileHandler.formatPsiFile(psiInFile)
        val actualSrc = psiInFile.text
        logger.info("The actual code is:\n$actualSrc")
        BasePlatformTestCase.assertEquals(expectedSrc, actualSrc)
    }
}
