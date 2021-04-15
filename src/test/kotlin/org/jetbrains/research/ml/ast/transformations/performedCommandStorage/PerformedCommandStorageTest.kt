package org.jetbrains.research.ml.ast.transformations.performedCommandStorage

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import junit.framework.TestCase
import org.jetbrains.research.ml.ast.transformations.anonymization.AnonymizationTransformation
import org.jetbrains.research.ml.ast.transformations.commands.CommandPerformer
import org.jetbrains.research.ml.ast.transformations.util.TransformationsWithSdkTest
import org.jetbrains.research.ml.ast.util.PsiFileHandler
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.nio.charset.Charset

@Ignore("Not supported yet")
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
//            CommentsRemovalTransformation,
            AnonymizationTransformation,
//            AugmentedAssignmentTransformation,
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
        val outPsiFile = psiHandler.getPsiFile(outFile!!)
        val commandStorage = CommandPerformer(inPsiFile, true)

        lateinit var actualAfterForwardTransformations: String
        lateinit var actualAfterForwardTransformationsFile: String
        lateinit var actualAfterBackwardTransformations: String
        val expectedAfterForwardTransformations = outPsiFile.text
        val expectedAfterBackwardTransformations = inPsiFile.text

        ApplicationManager.getApplication().invokeAndWait {
            transformations.forEach { it.forwardApply(inPsiFile, commandStorage) }
            actualAfterForwardTransformations = inPsiFile.text


            actualAfterForwardTransformationsFile = inPsiFile.containingFile.virtualFile.contentsToByteArray().toString(
                Charset.defaultCharset()
            )
            TestCase.assertEquals(actualAfterForwardTransformations, actualAfterForwardTransformationsFile)

            ApplicationManager.getApplication().runWriteAction {
                commandStorage.undoAllPerformedCommands()
            }
            val psiAfterBackwardTransformations = inPsiFile
//            val psiAfterBackwardTransformations = commandStorage.undoPerformedCommands()
            psiHandler.formatPsiFile(psiAfterBackwardTransformations)
            actualAfterBackwardTransformations = psiAfterBackwardTransformations.text
        }
        assertEquals(expectedAfterForwardTransformations, actualAfterForwardTransformations)
//        assertEquals(expectedAfterBackwardTransformations, actualAfterBackwardTransformations)
    }
}
