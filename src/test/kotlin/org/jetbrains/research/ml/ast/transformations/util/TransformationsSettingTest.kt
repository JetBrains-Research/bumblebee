/*
 * Copyright (c) 2020.  Anastasiia Birillo, Elena Lyulina
 */

package org.jetbrains.research.ml.ast.transformations.util

import com.intellij.psi.PsiElement
import org.jetbrains.research.ml.ast.transformations.util.TransformationsTestHelper.getInAndOutArray
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TransformationsSettingTest : TransformationsTest(getResourcesRootPath(::TransformationsSettingTest)) {

    companion object {
        @JvmStatic
        // TODO: how can we use only short name from file? Because I can use only the {index} parameter or the index
        //  of the parameter like {0} or {1}
        //  See: https://github.com/junit-team/junit4/wiki/Parameterized-tests
        //  Also see the issue: https://github.com/junit-team/junit5/issues/1309
        @Parameterized.Parameters(name = "{index}: ({0}, {1})")
        fun getTestData() = getInAndOutArray(::TransformationsSettingTest, resourcesRoot)
    }

    @Test
    fun `transformations setting test`() {
        // We are sure that inFile != null and outFile != null
        assertForwardTransformation(inFile!!, outFile!!) { println("Empty transformation") }
    }
}
