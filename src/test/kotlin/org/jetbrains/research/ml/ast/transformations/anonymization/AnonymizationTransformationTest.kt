package org.jetbrains.research.ml.ast.transformations.anonymization

import org.jetbrains.research.ml.ast.transformations.util.TransformationsTest
import org.jetbrains.research.ml.ast.transformations.util.TransformationsTestHelper.getBackwardTransformation
import org.jetbrains.research.ml.ast.transformations.util.TransformationsTestHelper.getForwardTransformationWrapper
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AnonymizationTransformationTest : TransformationsTest(getResourcesRootPath(::AnonymizationTransformationTest)) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: ({0}, {1})")
        fun getTestData() =
            getInAndOutArray(::AnonymizationTransformationTest, resourcesRoot)
    }

    @Test
    fun testForwardTransformation() {
        assertCodeTransformation(
            inFile!!,
            outFile!!,
            getForwardTransformationWrapper(AnonymizationTransformation::forwardApply)
        )
    }

    @Test
    fun testBackwardTransformation() {
        assertCodeTransformation(
            inFile!!,
            outFile!!,
            getBackwardTransformation(AnonymizationTransformation::forwardApply)
        )
    }
}
