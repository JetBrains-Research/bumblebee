package org.jetbrains.research.ml.ast.transformations.ifRedundantLinesRemoval

import org.jetbrains.research.ml.ast.transformations.util.TransformationsTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class IfRedundantLinesRemovalTransformationTest : TransformationsTest(getResourcesRootPath(::TransformationsTest)) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: ({0}, {1})")
        fun getTestData() = getInAndOutArray(::IfRedundantLinesRemovalTransformationTest)
    }

    @Test
    fun testForwardTransformation() {
        assertForwardTransformation(inFile!!, outFile!!, IfRedundantLinesRemovalTransformation::forwardApply)
    }

    @Test
    fun testBackwardTransformation() {
        assertBackwardTransformation(inFile!!, IfRedundantLinesRemovalTransformation::forwardApply)
    }
}
