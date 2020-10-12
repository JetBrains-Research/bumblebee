/*
 * Copyright (c) 2020.  Anastasiia Birillo, Elena Lyulina
 */

package org.jetbrains.research.ml.ast.transformations.util

import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Before
import org.junit.Ignore
import org.junit.jupiter.api.Assertions
import org.junit.runners.Parameterized
import java.io.File
import java.util.logging.Logger
import kotlin.reflect.KFunction

@Ignore
open class TransformationsTest(private val testDataRoot: String) : BasePlatformTestCase() {
    protected val LOG = Logger.getLogger(javaClass.name)

    // We should define the root resources folder
    override fun getTestDataPath() = testDataRoot

    @JvmField
    @Parameterized.Parameter(0)
    var inFile: File? = null

    @JvmField
    @Parameterized.Parameter(1)
    var outFile: File? = null

    companion object {
        // We can not get the root of the class resources automatically
        private const val resourcesRoot: String = "data"

        fun getInAndOutArray(
            cls: KFunction<TransformationsTest>,
            resourcesRootName: String = resourcesRoot,
        ): List<Array<Any>> {
            val inAndOutFilesMap = Util.getInAndOutFilesMap(getResourcesRootPath(cls, resourcesRootName))
            return inAndOutFilesMap.entries.map { (inFile, outFile) -> arrayOf(inFile, outFile) }
        }

        fun getResourcesRootPath(
            cls: KFunction<TransformationsTest>,
            resourcesRootName: String = resourcesRoot): String {
            return cls.javaClass.getResource(resourcesRootName).path
        }
    }

    /*
    *  TODO: can we do it better?
    *  The parametrized tests does not run the setUp method from BasePlatformTestCase and the myFixture variable
    *  is NULL. But we should init it
    * */
    @Before
    fun mySetUp() {
        super.setUp()
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
        val psiInFile = myFixture.configureByFile(inFile.name)
        // Todo: should we do return the new psiInFile after the transformation ot it will be changed?
        transformation(psiInFile, true)
        val actualSrc = psiInFile.text
        LOG.info("The actual code is:\n$actualSrc")
        Assertions.assertEquals(expectedSrc, actualSrc)
    }
}
