package org.jetbrains.research.ml.ast.transformations.emptyLinesRemoval

import org.jetbrains.research.ml.ast.transformations.util.TransformationsTest
import org.jetbrains.research.ml.ast.transformations.util.TransformationsTestHelper
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class EmptyLinesRemovalTransformationTest :
    TransformationsTest(getResourcesRootPath(::EmptyLinesRemovalTransformationTest)) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: ({0}, {1})")
        fun getTestData() =
            getInAndOutArray(::EmptyLinesRemovalTransformationTest, resourcesRoot)
    }

    @Test
    fun testForwardTransformation() {
        assertCodeTransformation(
            inFile!!,
            outFile!!,
            EmptyLinesRemovalTransformation::forwardApply
        )
    }

    @Test
    fun testBackwardTransformation() {
        assertCodeTransformation(
            inFile!!,
            inFile!!,
            TransformationsTestHelper.getBackwardTransformationWrapper(EmptyLinesRemovalTransformation::forwardApply)
        )
    }
}
