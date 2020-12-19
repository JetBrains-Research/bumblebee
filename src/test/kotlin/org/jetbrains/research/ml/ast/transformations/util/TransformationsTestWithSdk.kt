/*
 * Copyright (c) 2020.  Anastasiia Birillo, Elena Lyulina
 */

package org.jetbrains.research.ml.ast.transformations.util

import com.intellij.psi.PsiElement
import org.jetbrains.research.ml.ast.util.ParametrizedBaseWithSdkTest
import org.jetbrains.research.ml.ast.util.PsiFileHandler
import org.junit.Ignore
import org.junit.runners.Parameterized
import java.io.File

@Ignore
open class TransformationsTestWithSdk(testDataRoot: String) :
    ParametrizedBaseWithSdkTest(testDataRoot),
    ITransformationsTest {

    @JvmField
    @Parameterized.Parameter(0)
    var inFile: File? = null

    @JvmField
    @Parameterized.Parameter(1)
    var outFile: File? = null

    override fun assertCodeTransformation(
        inFile: File,
        outFile: File,
        transformation: (PsiElement, Boolean) -> Unit
    ) {
        TransformationsTestHelper.assertCodeTransformation(
            inFile,
            outFile,
            transformation,
            PsiFileHandler(myFixture, project),
            toCheckFileStructure = false
        )
    }
}
