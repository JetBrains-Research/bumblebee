package org.jetbrains.research.ml.ast.transformations.expressionUnification

import org.jetbrains.research.ml.ast.transformations.util.TransformationsTest
import org.jetbrains.research.ml.ast.transformations.util.TransformationsTestHelper.getBackwardTransformationWrapper
import org.jetbrains.research.ml.ast.transformations.util.TransformationsWithSdkTest
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Ignore("Not supported yet")
@RunWith(Parameterized::class)
class ExpressionUnificationTransformationTest :
    TransformationsWithSdkTest(getResourcesRootPath(::TransformationsTest)) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: ({0}, {1})")
        fun getTestData() = getInAndOutArray(::ExpressionUnificationTransformationTest, resourcesRoot)
    }

    @Test
    fun testForwardTransformation() {
        assertCodeTransformation(
            inFile!!,
            outFile!!,
            ExpressionUnificationTransformation::forwardApply
        )
    }

    @Test
    fun testBackwardTransformation() {
        assertCodeTransformation(
            inFile!!,
            inFile!!,
            getBackwardTransformationWrapper(ExpressionUnificationTransformation::forwardApply)
        )
    }
}
