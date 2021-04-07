package org.jetbrains.research.ml.ast.transformations.commentsRemoval

import org.jetbrains.research.ml.ast.transformations.PerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.anonymization.AnonymizationTransformation
import org.jetbrains.research.ml.ast.transformations.util.TransformationsTest
import org.jetbrains.research.ml.ast.transformations.util.TransformationsTestHelper
import org.jetbrains.research.ml.ast.transformations.util.TransformationsTestHelper.getBackwardTransformationWrapper
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CommentsRemovalTransformationTest : TransformationsTest(getResourcesRootPath(::TransformationsTest)) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: ({0}, {1})")
        fun getTestData()  = getInAndOutArray(::CommentsRemovalTransformationTest, resourcesRoot).
        filter { it.all { f -> f.name.contains("2") } }
    }

    @Test
    fun testForwardTransformation() {
        assertCodeTransformation(
            inFile!!,
            outFile!!,
            CommentsRemovalTransformation::forwardApply
        )
    }

    @Test
    fun testBackwardTransformation() {
        assertCodeTransformation(
            inFile!!,
            inFile!!,
            getBackwardTransformationWrapper(CommentsRemovalTransformation::forwardApply)
        )
    }

    @Test
    fun testCommandStorage() {
        assertCodeTransformation(
            inFile!!,
            inFile!!,
            TransformationsTestHelper.getCommandStorageTransformationWrapper(
                ::PerformedCommandStorage,
                CommentsRemovalTransformation::forwardApply
            )
        )
    }
}
