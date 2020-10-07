/*
 * Copyright (c) 2020.  Anastasiia Birillo, Elena Lyulina
 */

package org.jetbrains.research.ml.ast.transformations.util

import com.intellij.psi.PsiElement
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.provider.Arguments
import java.io.File
import java.util.logging.Logger
import kotlin.reflect.KFunction

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class TransformationsTest {
    protected val LOG = Logger.getLogger(javaClass.name)

    companion object {
        fun getInAndOutArgumentsArray(
                resourcesRootName: String,
                cls: KFunction<TransformationsTest>
        ): Array<Arguments> {
            val resourcesRootPath = cls.javaClass.getResource(resourcesRootName).path
            val inAndOutFilesMap = Util.getInAndOutFilesMap(resourcesRootPath)
            return inAndOutFilesMap.entries.map { (inFile, outFile) -> Arguments.of(inFile, outFile) }.toTypedArray()
        }
    }

    protected fun assertCodeTransformation(
            inFile: File,
            outFile: File,
            transformation: (PsiElement, Boolean) -> Unit
    ) {
        LOG.info("The current input file is: ${inFile.path}")
        LOG.info("The current output file is: ${outFile.path}")
        val expectedSrc = Util.getContentFromFile(outFile)
        LOG.info("The expected code is:\n$expectedSrc")
        TODO("create PSI by inFile, apply transformation and Assertions.assertEquals(expectedSrc, actualSrc)")
    }
}