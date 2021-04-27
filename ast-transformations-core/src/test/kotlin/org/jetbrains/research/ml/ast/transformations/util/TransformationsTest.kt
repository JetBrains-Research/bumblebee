/*
 * Copyright (c) 2020.  Anastasiia Birillo, Elena Lyulina
 */

package org.jetbrains.research.ml.ast.transformations.util

import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.research.ml.ast.util.*
import org.junit.Ignore
import org.junit.runners.Parameterized
import java.io.File
import kotlin.reflect.KFunction

@Ignore
open class TransformationsTest(testDataRoot: String) : ParametrizedBaseTest(testDataRoot), ITransformationsTest {
    override lateinit var codeStyleManager: CodeStyleManager

    @JvmField
    @Parameterized.Parameter(0)
    var inFile: File? = null

    @JvmField
    @Parameterized.Parameter(1)
    var outFile: File? = null

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

    fun assertInverseCodeTransformation(
        inFile: File,
        outFile: File,
        transformation: TransformationDelayed,
        inverseTransformation: TransformationDelayed
    ) {
        TransformationsTestHelper.assertCodeInverseTransformation(
            inFile,
            outFile,
            transformation,
            inverseTransformation,
            PsiFileHandler(myFixture, project)
        )
    }
}
