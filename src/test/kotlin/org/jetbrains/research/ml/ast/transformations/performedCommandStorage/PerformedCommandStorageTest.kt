package org.jetbrains.research.ml.ast.transformations.performedCommandStorage

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.research.ml.ast.transformations.PerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.anonymization.AnonymizationTransformation
import org.jetbrains.research.ml.ast.transformations.augmentedAssignment.AugmentedAssignmentTransformation
import org.jetbrains.research.ml.ast.transformations.commentsRemoval.CommentsRemovalTransformation
import org.jetbrains.research.ml.ast.transformations.deadcode.DeadCodeRemovalTransformation
import org.jetbrains.research.ml.ast.transformations.ifRedundantLinesRemoval.IfRedundantLinesRemovalTransformation
import org.jetbrains.research.ml.ast.transformations.multipleOperatorComparison.MultipleOperatorComparisonTransformation
import org.jetbrains.research.ml.ast.transformations.multipleTargetAssignment.MultipleTargetAssignmentTransformation
import org.jetbrains.research.ml.ast.transformations.util.TransformationsTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class PerformedCommandStorageTest : TransformationsTest(getResourcesRootPath(::PerformedCommandStorageTest)) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: ({0}, {1})")
        fun getTestData() = getInAndOutArray(::PerformedCommandStorageTest)
    }

    @Test
    fun `test applying all transformations consecutively`() {
        val transformations = arrayListOf(
            CommentsRemovalTransformation,
            AnonymizationTransformation,
            AugmentedAssignmentTransformation,
            DeadCodeRemovalTransformation,
            MultipleOperatorComparisonTransformation,
            MultipleTargetAssignmentTransformation,
            IfRedundantLinesRemovalTransformation
        )
        val inPsiFile = getPsiFile(inFile!!)
        val outPsiFile = getPsiFile(outFile!!)
        val commandStorage = PerformedCommandStorage(inPsiFile)

        lateinit var actualAfterForwardTransformations: String
        lateinit var actualAfterBackwardTransformations: String
        val expectedAfterForwardTransformations = outPsiFile.text
        val expectedAfterBackwardTransformations = inPsiFile.text

        ApplicationManager.getApplication().invokeAndWait {
            transformations.forEach { it.forwardApply(inPsiFile, commandStorage) }
            actualAfterForwardTransformations = inPsiFile.text
            val psiAfterBackwardTransformations = commandStorage.undoPerformedCommands()
            formatPsiFile(psiAfterBackwardTransformations)
            actualAfterBackwardTransformations = psiAfterBackwardTransformations.text
        }
        assertEquals(expectedAfterForwardTransformations, actualAfterForwardTransformations)
        assertEquals(expectedAfterBackwardTransformations, actualAfterBackwardTransformations)
    }
}
