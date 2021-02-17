/*
 * Copyright (c) 2020.  Anastasiia Birillo, Elena Lyulina
 */

package org.jetbrains.research.ml.ast.transformations.util

import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.research.ml.ast.util.*
import org.junit.Before
import org.junit.Ignore
import org.junit.runners.Parameterized
import java.io.File
import kotlin.reflect.KFunction

@Ignore
open class TransformationsWithSdkTest(testDataRoot: String) :
    ParametrizedBaseWithSdkTest(testDataRoot),
    ITransformationsTest {
    override lateinit var codeStyleManager: CodeStyleManager

    companion object {
        fun getInAndOutArray(
            cls: KFunction<TransformationsWithSdkTest>,
            resourcesRootName: String = resourcesRoot,
        ): List<Array<File>> {
            val inAndOutFilesMap = FileTestUtil.getInAndOutFilesMap(
                getResourcesRootPath(cls, resourcesRootName),
                outFormat = TestFileFormat("out", Extension.Py, Type.Output)
            )
            return inAndOutFilesMap.entries.map { (inFile, outFile) -> arrayOf(inFile, outFile!!) }
        }
    }

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

    override fun assertCodeTransformation(
        inFile: File,
        outFile: File,
        transformation: TransformationDelayed
    ) {
        TransformationsTestHelper.assertCodeTransformation(
            inFile,
            outFile,
            transformation,
            PsiFileHandler(myFixture, project)
        )
    }
}
