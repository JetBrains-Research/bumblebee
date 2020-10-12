/*
 * Copyright (c) 2020.  Anastasiia Birillo, Elena Lyulina
 */

package org.jetbrains.research.ml.ast.transformations.util

import com.intellij.psi.PsiElement
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized


@RunWith(Parameterized::class)
class TransformationsSettingTest : TransformationsTest(getResourcesRootPath(::TransformationsTest)) {

    companion object {
        @JvmStatic
        // TODO: how can we use only short name from file? Because I can use only the {index} parameter or the index
        //  of the parameter like {0} or {1}
        //  See: https://stackoverflow.com/questions/650894/changing-names-of-parameterized-tests
        @Parameterized.Parameters(name = "{0} : {1}")
        fun getTestData() = getInAndOutArray(::TransformationsTest)
    }

    @Test
    fun `transformations setting test`() {
        // We are sure that inFile != null and outFile != null
        assertCodeTransformation(inFile!!, outFile!!) { _: PsiElement, _: Boolean -> println("Empty transformation") }
    }
}