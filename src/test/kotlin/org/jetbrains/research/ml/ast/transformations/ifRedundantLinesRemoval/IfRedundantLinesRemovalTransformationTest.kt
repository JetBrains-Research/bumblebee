package org.jetbrains.research.ml.ast.transformations.ifRedundantLinesRemoval

import org.jetbrains.research.ml.ast.transformations.util.TransformationsTest
import org.jetbrains.research.ml.ast.transformations.util.TransformationsTestHelper.getBackwardTransformationWrapper
import org.jetbrains.research.ml.ast.transformations.util.TransformationsWithSdkTest
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Ignore("Not supported yet")
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
            IfRedundantLinesRemovalTransformation::forwardApply
        )
    }

    @Test
    fun testBackwardTransformation() {
        assertCodeTransformation(
            inFile!!,
            inFile!!,
            getBackwardTransformationWrapper(IfRedundantLinesRemovalTransformation::forwardApply)
        )
    }
}
