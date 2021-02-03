package org.jetbrains.research.ml.ast.transformations.expressionUnification

import org.jetbrains.research.ml.ast.transformations.util.TransformationsWithSdkTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ExpressionUnificationTransformationTest :
    TransformationsWithSdkTest(getResourcesRootPath(::ExpressionUnificationTransformationTest)) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: ({0}, {1})")
        fun getTestData() = getInAndOutArray(::ExpressionUnificationTransformationTest)
    }

    @Test
    fun testForwardTransformation() {
        assertForwardTransformation(inFile!!, outFile!!, ExpressionUnificationTransformation::forwardApply)
    }

    @Test
    fun testBackwardTransformation() {
        assertBackwardTransformation(
            inFile!!,
            ExpressionUnificationTransformation::forwardApply
        )
    }
}
