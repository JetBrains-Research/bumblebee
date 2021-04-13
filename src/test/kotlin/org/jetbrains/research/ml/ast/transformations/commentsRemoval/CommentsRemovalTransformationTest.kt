package org.jetbrains.research.ml.ast.transformations.commentsRemoval

import org.jetbrains.research.ml.ast.transformations.commands.CommandPerformer
import org.jetbrains.research.ml.ast.transformations.util.TransformationsTest
import org.jetbrains.research.ml.ast.transformations.util.TransformationsTestHelper
import org.jetbrains.research.ml.ast.transformations.util.TransformationsTestHelper.getBackwardTransformationWrapper
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Ignore("Not supported yet")
@RunWith(Parameterized::class)
class CommentsRemovalTransformationTest : TransformationsTest(getResourcesRootPath(::TransformationsTest)) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: ({0}, {1})")
        fun getTestData()  = getInAndOutArray(::CommentsRemovalTransformationTest, resourcesRoot)
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
                ::CommandPerformer,
                CommentsRemovalTransformation::forwardApply
            )
        )
    }
}
