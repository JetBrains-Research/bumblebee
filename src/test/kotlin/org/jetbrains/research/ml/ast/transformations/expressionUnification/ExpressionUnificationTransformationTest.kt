package org.jetbrains.research.ml.ast.transformations.expressionUnification

import org.jetbrains.research.ml.ast.transformations.util.TransformationsTestWithSdk
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ExpressionUnificationTransformationTest :
    TransformationsTestWithSdk(getResourcesRootPath(::ExpressionUnificationTransformationTest)) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: ({0}, {1})")
        fun getTestData() = getInAndOutArray(::ExpressionUnificationTransformationTest)
    }

    @Test
    fun testForwardTransformation() {
        assertCodeTransformation(inFile!!, outFile!!) { psiTree, toStoreMetadata ->
            val transformation = ExpressionUnificationTransformation()
            transformation.apply(psiTree, toStoreMetadata)
        }
    }
}
