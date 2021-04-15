package org.jetbrains.research.ml.ast.transformations.performedCommandStorage

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiElement
import junit.framework.TestCase
import org.jetbrains.research.ml.ast.transformations.anonymization.AnonymizationTransformation
import org.jetbrains.research.ml.ast.transformations.augmentedAssignment.AugmentedAssignmentTransformation
import org.jetbrains.research.ml.ast.transformations.commands.CommandPerformer
import org.jetbrains.research.ml.ast.transformations.commentsRemoval.CommentsRemovalTransformation
import org.jetbrains.research.ml.ast.transformations.util.TransformationsWithSdkTest
import org.jetbrains.research.ml.ast.util.PsiFileHandler
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.nio.charset.Charset

@RunWith(Parameterized::class)
class PerformedCommandStorageTest : TransformationsWithSdkTest(getResourcesRootPath(::PerformedCommandStorageTest)) {

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
            AugmentedAssignmentTransformation
//            DeadCodeRemovalTransformation,
//            ConstantFoldingTransformation,
//            MultipleOperatorComparisonTransformation,
//            MultipleTargetAssignmentTransformation,
//            IfRedundantLinesRemovalTransformation,
//            ComparisonUnificationTransformation,
//            OuterNotEliminationTransformation
        )

        val psiHandler = PsiFileHandler(myFixture, project)

        val inPsiFile = psiHandler.getPsiFile(inFile!!)
        val inDocument = FileDocumentManager.getInstance().getDocument(inPsiFile.virtualFile)!!

        val outPsiFile = psiHandler.getPsiFile(outFile!!)
        val commandStorage = CommandPerformer(inPsiFile, true)

        lateinit var actualAfterForwardTransformations: String
        lateinit var actualAfterBackwardTransformations: String
        val expectedAfterForwardTransformations = outPsiFile.text
        val expectedAfterBackwardTransformations = inPsiFile.text

        ApplicationManager.getApplication().invokeAndWait {
            transformations.forEach {
                it.forwardApply(inPsiFile, commandStorage)
                FileDocumentManager.getInstance().saveDocument(inDocument)
            }
            actualAfterForwardTransformations = inPsiFile.text
            commandStorage.undoAllPerformedCommands()
            val psiAfterBackwardTransformations = inPsiFile
            psiHandler.formatPsiFile(psiAfterBackwardTransformations)
            actualAfterBackwardTransformations = inPsiFile.text
        }
        assertEquals(expectedAfterForwardTransformations, actualAfterForwardTransformations)
        assertEquals(expectedAfterBackwardTransformations, actualAfterBackwardTransformations)
    }
}
