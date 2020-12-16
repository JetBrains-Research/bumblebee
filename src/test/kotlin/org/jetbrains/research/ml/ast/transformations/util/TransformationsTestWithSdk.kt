package org.jetbrains.research.ml.ast.transformations.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.research.ml.ast.util.FileTestUtil
import org.jetbrains.research.ml.ast.util.ParametrizedBaseWithSdkTest
import org.junit.Before
import org.junit.Ignore
import org.junit.runners.Parameterized
import java.io.File
import kotlin.reflect.KFunction

@Ignore
open class TransformationsTestWithSdk(testDataRoot: String) : ParametrizedBaseWithSdkTest(testDataRoot) {
    private lateinit var codeStyleManager: CodeStyleManager

    @JvmField
    @Parameterized.Parameter(0)
    var inFile: File? = null

    @JvmField
    @Parameterized.Parameter(1)
    var outFile: File? = null

    @Before
    override fun mySetUp() {
        super.mySetUp()
        codeStyleManager = CodeStyleManager.getInstance(project)
    }

    companion object {
        fun getInAndOutArray(
            cls: KFunction<TransformationsTestWithSdk>,
            resourcesRootName: String = resourcesRoot,
        ): List<Array<File>> {
            val inAndOutFilesMap = FileTestUtil.getInAndOutFilesMap(
                getResourcesRootPath(
                    cls,
                    resourcesRootName
                )
            )
            return inAndOutFilesMap.entries.map { (inFile, outFile) -> arrayOf(inFile, outFile) }
        }
    }

    protected fun assertCodeTransformation(
        inFile: File,
        outFile: File,
        transformation: (PsiElement, Boolean) -> Unit
    ) {
        LOG.info("The current input file is: ${inFile.path}")
        LOG.info("The current output file is: ${outFile.path}")
        val psiInFile = getPsiFile(inFile)
        val expectedPsiInFile = getPsiFile(outFile)
        val expectedSrc = expectedPsiInFile.text
        LOG.info("The expected code is:\n$expectedSrc")
        ApplicationManager.getApplication().invokeAndWait {
            transformation(psiInFile, true)
//            PsiTestUtil.checkFileStructure(psiInFile)
        }
        formatPsiFile(psiInFile)
        val actualSrc = psiInFile.text
        LOG.info("The actual code is:\n$actualSrc")
        assertEquals(expectedSrc, actualSrc)
    }

    private fun getPsiFile(file: File, toReformatFile: Boolean = true): PsiFile {
        val psiFile = myFixture.configureByFile(file.path)
        if (toReformatFile) {
            formatPsiFile(psiFile)
        }
        return psiFile
    }

    private fun formatPsiFile(psi: PsiElement) {
        WriteCommandAction.runWriteCommandAction(project) { // reformat the expected file
            codeStyleManager.reformat(psi)
        }
    }
}
