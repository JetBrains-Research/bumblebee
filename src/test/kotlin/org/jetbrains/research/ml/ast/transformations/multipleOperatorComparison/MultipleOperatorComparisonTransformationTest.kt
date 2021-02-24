package org.jetbrains.research.ml.ast.transformations.multipleOperatorComparison

import org.jetbrains.research.ml.ast.transformations.util.TransformationsTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class MultipleOperatorComparisonTransformationTest :
    TransformationsTest(getResourcesRootPath(::MultipleOperatorComparisonTransformationTest)) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: ({0}, {1})")
        fun getTestData() = getInAndOutArray(::MultipleOperatorComparisonTransformationTest, resourcesRoot)
    }

    @Test
    fun testForwardTransformation() {
        assertForwardTransformation(inFile!!, outFile!!, MultipleOperatorComparisonTransformation::forwardApply)
    }

    @Test
    fun testBackwardTransformation() {
        assertBackwardTransformation(
            inFile!!,
            MultipleOperatorComparisonTransformation::forwardApply
        )
    }
}
