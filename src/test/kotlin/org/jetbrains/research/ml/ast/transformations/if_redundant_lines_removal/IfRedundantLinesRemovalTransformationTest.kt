package org.jetbrains.research.ml.ast.transformations.if_redundant_lines_removal

import org.jetbrains.research.ml.ast.transformations.util.TransformationsTest
import org.jetbrains.research.ml.ast.transformations.util.TransformationsTestHelper.getBackwardTransformation
import org.jetbrains.research.ml.ast.transformations.util.TransformationsTestHelper.getForwardTransformationWrapper
import org.jetbrains.research.ml.ast.transformations.util.TransformationsWithSdkTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class IfRedundantLinesRemovalTransformationTest : TransformationsWithSdkTest(
    getResourcesRootPath(::TransformationsTest)
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: ({0}, {1})")
        fun getTestData() = getInAndOutArray(::IfRedundantLinesRemovalTransformationTest)
    }

    @Test
    fun testForwardTransformation() {
        assertCodeTransformation(
            inFile!!,
            outFile!!,
            getForwardTransformationWrapper(IfRedundantLinesRemovalTransformation::forwardApply)
        )
    }

    @Test
    fun testBackwardTransformation() {
        assertCodeTransformation(
            inFile!!,
            outFile!!,
            getBackwardTransformation(IfRedundantLinesRemovalTransformation::forwardApply)
        )
    }
}