/*
 * Copyright (c) 2020.  Anastasiia Birillo, Elena Lyulina
 */

package org.jetbrains.research.ml.ast.transformations.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.research.ml.ast.util.FileTestUtil
import org.jetbrains.research.ml.ast.util.ParametrizedBaseTest
import org.junit.Ignore
import org.junit.runners.Parameterized
import java.io.File
import kotlin.reflect.KFunction

@Ignore
open class TransformationsTest(testDataRoot: String) : ParametrizedBaseTest(testDataRoot) {
    protected lateinit var codeStyleManager: CodeStyleManager

    @JvmField
    @Parameterized.Parameter(0)
    var inFile: File? = null

    @JvmField
    @Parameterized.Parameter(1)
    var outFile: File? = null

    override fun mySetUp() {
        super.mySetUp()
        codeStyleManager = CodeStyleManager.getInstance(project)
    }

    companion object {
        fun getInAndOutArray(
            cls: KFunction<TransformationsTest>,
            resourcesRootName: String = resourcesRoot,
        ): List<Array<File>> {
            val inAndOutFilesMap = FileTestUtil.getInAndOutFilesMap(getResourcesRootPath(cls, resourcesRootName))
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
        val psiInFile = getPsiFile(inFile.name)
        val expectedPsiInFile = getPsiFile(outFile.name)
        val expectedSrc = expectedPsiInFile.text
        LOG.info("The expected code is:\n$expectedSrc")
        ApplicationManager.getApplication().invokeAndWait {
            transformation(psiInFile, true)
        }
        val actualSrc = psiInFile.text
        LOG.info("The actual code is:\n$actualSrc")
        assertEquals(expectedSrc, actualSrc)
    }

    private fun getPsiFile(filename: String, toReformatFile: Boolean = true): PsiFile {
        val psiFile = myFixture.configureByFile(filename)
        if (toReformatFile) {
            WriteCommandAction.runWriteCommandAction(project) {
                codeStyleManager.reformat(psiFile)
            }
        }
        return psiFile
    }
}
