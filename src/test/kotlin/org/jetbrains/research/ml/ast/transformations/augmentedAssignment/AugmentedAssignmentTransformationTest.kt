package org.jetbrains.research.ml.ast.transformations.augmentedAssignment

import org.jetbrains.research.ml.ast.transformations.util.TransformationsTest
import org.jetbrains.research.ml.ast.transformations.util.TransformationsTestHelper.getInAndOutArray
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AugmentedAssignmentTransformationTest :
    TransformationsTest(getResourcesRootPath(::AugmentedAssignmentTransformationTest)) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: ({0}, {1})")
        fun getTestData() = getInAndOutArray(::AugmentedAssignmentTransformationTest, resourcesRoot)
    }

    @Test
    fun testForwardTransformation() {
        assertForwardTransformation(inFile!!, outFile!!, AugmentedAssignmentTransformation::forwardApply)
    }

    @Test
    fun testBackwardTransformation() {
        assertBackwardTransformation(
            inFile!!,
            AugmentedAssignmentTransformation::forwardApply
        )
    }
}
