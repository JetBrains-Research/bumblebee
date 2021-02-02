/*
 * Copyright (c) 2020.  Anastasiia Birillo, Elena Lyulina
 */

package org.jetbrains.research.ml.ast.transformations.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.testFramework.PsiTestUtil
import org.jetbrains.research.ml.ast.transformations.PerformedCommandStorage
import org.jetbrains.research.ml.ast.util.*
import org.junit.Before
import org.junit.Ignore
import org.junit.runners.Parameterized
import java.io.File
import kotlin.reflect.KFunction

@Ignore
open class TransformationsTest(testDataRoot: String) : ParametrizedBaseTest(testDataRoot) {
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
            cls: KFunction<TransformationsTest>,
            resourcesRootName: String = resourcesRoot,
        ): List<Array<File>> {
            val inAndOutFilesMap = FileTestUtil.getInAndOutFilesMap(
                getResourcesRootPath(cls, resourcesRootName),
                outFormat = TestFileFormat("out", Extension.Py, Type.Output)
            )
            return inAndOutFilesMap.entries.map { (inFile, outFile) -> arrayOf(inFile, outFile!!) }
        }
    }

    private fun applyTransformation(
        psiFile: PsiFile,
        transformation: (PsiFile, PerformedCommandStorage?) -> PsiElement?,
        commandStorage: PerformedCommandStorage? = null
    ): String {
        lateinit var actualSrc: String
        ApplicationManager.getApplication().invokeAndWait {
            val actualPsiFile = transformation(psiFile, commandStorage)
            require(actualPsiFile != null) { "Got null instead actual psi file!" }
            PsiTestUtil.checkFileStructure(psiFile)
            formatPsiFile(actualPsiFile)
            actualSrc = actualPsiFile.text
        }
        return actualSrc
    }

    private fun assertCode(expectedSrc: String, actualSrc: String) {
        LOG.info("The expected code is:\n$expectedSrc")
        LOG.info("The actual code is:\n$actualSrc")
        assertEquals(expectedSrc, actualSrc)
    }

    protected fun assertForwardTransformation(
        inFile: File,
        outFile: File,
        transformation: (PsiElement) -> Unit
    ) {
        LOG.info("The current input file is: ${inFile.path}")
        LOG.info("The current output file is: ${outFile.path}")
        val psiInFile = getPsiFile(inFile)
        val expectedPsiInFile = getPsiFile(outFile)
        val actualSrc = applyTransformation(
            psiInFile,
            transformation = { psi: PsiFile, cs: PerformedCommandStorage? ->
                transformation(psi)
                psi
            }
        )
        assertCode(expectedPsiInFile.text, actualSrc)
    }

    protected fun assertBackwardTransformation(
        inFile: File,
        forwardTransformation: (PsiElement, PerformedCommandStorage?) -> Unit
    ) {
        LOG.info("The current input file is: ${inFile.path}")
        val psiInFile = getPsiFile(inFile)
        LOG.info("The input code is: ${psiInFile.text}")
        val actualSrc = applyTransformation(
            psiInFile,
            commandStorage = PerformedCommandStorage(psiInFile),
            transformation = { psi: PsiFile, cs: PerformedCommandStorage? ->
                forwardTransformation(psi, cs)
                PsiTestUtil.checkFileStructure(psi)
                cs?.undoPerformedCommands()
            }
        )
        // Expected Psi should be the same as the input Psi
        assertCode(psiInFile.text, actualSrc)
    }

    protected fun getPsiFile(file: File, toReformatFile: Boolean = true): PsiFile {
        val psiFile = myFixture.configureByFile(file.path)
        if (toReformatFile) {
            formatPsiFile(psiFile)
        }
        return psiFile
    }

    protected fun formatPsiFile(psi: PsiElement) {
        WriteCommandAction.runWriteCommandAction(project) { // reformat the expected file
            codeStyleManager.reformat(psi)
        }
    }
}
