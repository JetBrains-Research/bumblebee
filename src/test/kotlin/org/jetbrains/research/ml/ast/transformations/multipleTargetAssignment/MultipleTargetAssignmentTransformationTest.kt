package org.jetbrains.research.ml.ast.transformations.multipleTargetAssignment

import org.jetbrains.research.ml.ast.transformations.util.BaseTransformationsTestHelper.Companion.getInAndOutArray
import org.jetbrains.research.ml.ast.transformations.util.TransformationsTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class MultipleTargetAssignmentTransformationTest : TransformationsTest(getResourcesRootPath(::TransformationsTest)) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: ({0}, {1})")
        fun getTestData() = getInAndOutArray(::MultipleTargetAssignmentTransformationTest, resourcesRoot)
    }

    @Test
    fun testForwardTransformation() {
        assertCodeTransformation(inFile!!, outFile!!) { psiTree, toStoreMetadata ->
            val transformation = MultipleTargetAssignmentTransformation()
            transformation.apply(psiTree, toStoreMetadata)
        }
    }
}
