package org.jetbrains.research.ml.ast.transformations.augmentedAssignment

import org.jetbrains.research.ml.ast.transformations.util.BaseTransformationsTestHelper.Companion.getInAndOutArray
import org.jetbrains.research.ml.ast.transformations.util.TransformationsTest
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
        assertCodeTransformation(inFile!!, outFile!!) { psiTree, toStoreMetadata ->
            val transformation = AugmentedAssignmentTransformation()
            transformation.apply(psiTree, toStoreMetadata)
        }
    }
}
