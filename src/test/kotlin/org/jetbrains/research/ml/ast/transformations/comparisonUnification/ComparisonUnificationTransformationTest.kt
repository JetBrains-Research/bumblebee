package org.jetbrains.research.ml.ast.transformations.comparisonUnification

import org.jetbrains.research.ml.ast.transformations.util.TransformationsTest
import org.jetbrains.research.ml.ast.transformations.util.TransformationsTestHelper.getBackwardTransformation
import org.jetbrains.research.ml.ast.transformations.util.TransformationsTestHelper.getForwardTransformationWrapper
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ComparisonUnificationTransformationTest : TransformationsTest(getResourcesRootPath(::TransformationsTest)) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: ({0}, {1})")
        fun getTestData() = getInAndOutArray(::ComparisonUnificationTransformationTest)
    }

    @Test
    fun testForwardTransformation() {
        assertCodeTransformation(
            inFile!!,
            outFile!!,
            getForwardTransformationWrapper(ComparisonUnificationTransformation::forwardApply)
        )
    }

    @Test
    fun testBackwardTransformation() {
        assertCodeTransformation(
            inFile!!,
            outFile!!,
            getBackwardTransformation(ComparisonUnificationTransformation::forwardApply)
        )
    }
}
