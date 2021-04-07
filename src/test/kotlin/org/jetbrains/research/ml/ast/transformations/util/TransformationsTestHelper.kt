package org.jetbrains.research.ml.ast.transformations.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.research.ml.ast.transformations.IPerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.PerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.Transformation
import org.jetbrains.research.ml.ast.util.FileTestUtil
import org.jetbrains.research.ml.ast.util.ParametrizedBaseTest
import org.jetbrains.research.ml.ast.util.PsiFileHandler
import java.io.File
import java.nio.charset.Charset
import java.util.logging.Logger
import kotlin.reflect.KFunction

typealias TransformationDelayed = (PsiElement) -> Unit

/**
 * We want to ensure that all tests classes for transformations have the required functionality
 */
interface ITransformationsTest {
    var codeStyleManager: CodeStyleManager

    fun assertCodeTransformation(
        inFile: File,
        outFile: File,
        transformation: TransformationDelayed
    )
}

/**
 * We moved out common functions to make classes easier for tests with SDK and without SDK
 */
object TransformationsTestHelper {
    private val logger = Logger.getLogger(javaClass.name)

    fun getInAndOutArray(cls: KFunction<ParametrizedBaseTest>, resourcesRootName: String): List<Array<File?>> {
        val inAndOutFilesMap = FileTestUtil.getInAndOutFilesMap(
            ParametrizedBaseTest.getResourcesRootPath(cls, resourcesRootName)
        )
        return inAndOutFilesMap.entries.map { (inFile, outFile) -> arrayOf(inFile, outFile) }
    }

    fun assertCodeTransformation(
        inFile: File,
        outFile: File,
        transformation: TransformationDelayed,
        fileHandler: PsiFileHandler
    ) {
        logger.info("The current input file is: ${inFile.path}")
        logger.info("The current output file is: ${outFile.path}")
        val psiInFile = fileHandler.getPsiFile(inFile)
        val expectedPsiInFile = if (inFile.path == outFile.path) {
            psiInFile
        } else {
            fileHandler.getPsiFile(outFile)
        }
        val expectedSrc = expectedPsiInFile.text
        logger.info("The expected code is:\n$expectedSrc")
        ApplicationManager.getApplication().invokeAndWait {
            transformation(psiInFile)
            PsiTestUtil.checkFileStructure(psiInFile)
        }
        fileHandler.formatPsiFile(psiInFile)
        val actualSrc = psiInFile.text
        logger.info("The actual code is:\n$actualSrc")
        BasePlatformTestCase.assertEquals(expectedSrc, actualSrc)
    }

    fun getBackwardTransformationWrapper(
        forwardTransformation: (PsiElement, cs: IPerformedCommandStorage?) -> Unit
    ): TransformationDelayed =
        { psi: PsiElement ->
            val commandStorage = PerformedCommandStorage(psi)
            forwardTransformation(psi, commandStorage)
            PsiTestUtil.checkFileStructure(psi as PsiFile)
            ApplicationManager.getApplication().runWriteAction<PsiElement> {
                commandStorage.undoPerformedCommands()
            }
        }

    fun getCommandStorageTransformationWrapper(
        commandStorageToTest: (PsiElement) -> IPerformedCommandStorage,
        forwardTransformation: (PsiElement, cs: IPerformedCommandStorage?) -> Unit
    ): TransformationDelayed =
        { psi: PsiElement ->
//          This commandStorage will perform all commands as commandStorageToTest does but with additional check between commands
            val commandStorage = TestPerformedCommandStorage(commandStorageToTest(psi))

            forwardTransformation(psi, commandStorage)
            PsiTestUtil.checkFileStructure(psi as PsiFile)
            ApplicationManager.getApplication().runWriteAction<PsiElement> {
                commandStorage.undoPerformedCommands()
            }
        }
}


class TestPerformedCommandStorage(private val storageToTest: IPerformedCommandStorage) : IPerformedCommandStorage {
    override val psiTree: PsiElement = storageToTest.psiTree

    private data class PsiText(private val psiTree: PsiElement) {
        val psiText = psiTree.text
        val virtualFileText = psiTree.containingFile.virtualFile.contentsToByteArray().toString(Charset.defaultCharset())
    }

    override fun performCommand(command: () -> Unit, description: String) {
        val textBeforeCommand = PsiText(storageToTest.psiTree)
        storageToTest.performCommand(command, description)

//      perform undo and assert equals
        undoPerformedCommands(1)
        val textAfterUndoCommand = PsiText(storageToTest.psiTree)

//        PsiTestUtil.checkFileStructure(storageToTest.psiTree as PsiFile)

//      not sure which text I should assert.....
//        BasePlatformTestCase.assertEquals(
//            "Psi texts after undoing $description don't match",
//            textBeforeCommand.psiText,
//            textAfterUndoCommand.psiText
//        )
//        BasePlatformTestCase.assertEquals(
//            "Virtual file texts after undoing $description don't match",
//            textBeforeCommand.virtualFileText,
//            textAfterUndoCommand.virtualFileText
//        )

//      perform command again
        storageToTest.performCommand(command, description)
    }

    override fun undoPerformedCommands(maxN: Int): PsiElement {
        return storageToTest.undoPerformedCommands(maxN)
    }

    override fun performUndoableCommand(command: () -> Unit, undoCommand: () -> Unit, description: String) {
        storageToTest.performUndoableCommand(command, undoCommand, description)
    }

}
