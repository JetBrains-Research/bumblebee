package org.jetbrains.research.ml.ast.transformations.anonymization

import org.jetbrains.research.ml.ast.transformations.commands.CommandPerformer
import org.jetbrains.research.ml.ast.transformations.util.TransformationsTest
import org.jetbrains.research.ml.ast.transformations.util.TransformationsTestHelper.getBackwardTransformationWrapper
import org.jetbrains.research.ml.ast.transformations.util.TransformationsTestHelper.getCommandStorageTransformationWrapper
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AnonymizationTransformationTest : TransformationsTest(getResourcesRootPath(::AnonymizationTransformationTest)) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: ({0}, {1})")
        fun getTestData() = getInAndOutArray(::AnonymizationTransformationTest, resourcesRoot).filter {
            it[0].name.startsWith("in_1_forward_name_clash")
        }
    }

    @Test
    fun testForwardTransformation() {
        assertCodeTransformation(
            inFile!!,
            outFile!!,
            AnonymizationTransformation::forwardApply
        )
    }

    @Test
    fun testBackwardTransformation() {
        assertCodeTransformation(
            inFile!!,
            inFile!!,
            getBackwardTransformationWrapper(AnonymizationTransformation::forwardApply)
        )
    }

    @Test
    fun testCommandStorage() {
        assertCodeTransformation(
            inFile!!,
            inFile!!,
            getCommandStorageTransformationWrapper(
                ::CommandPerformer,
                AnonymizationTransformation::forwardApply
            )
        )
    }
}
