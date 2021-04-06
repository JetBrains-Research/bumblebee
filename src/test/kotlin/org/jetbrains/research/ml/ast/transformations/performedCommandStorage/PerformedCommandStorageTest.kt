package org.jetbrains.research.ml.ast.transformations.performedCommandStorage

import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import junit.framework.TestCase
import org.jetbrains.research.ml.ast.transformations.PerformedCommandStorage
import org.jetbrains.research.ml.ast.transformations.anonymization.AnonymizationTransformation
import org.jetbrains.research.ml.ast.transformations.augmentedAssignment.AugmentedAssignmentTransformation
import org.jetbrains.research.ml.ast.transformations.commentsRemoval.CommentsRemovalTransformation
import org.jetbrains.research.ml.ast.transformations.comparisonUnification.ComparisonUnificationTransformation
import org.jetbrains.research.ml.ast.transformations.constantfolding.ConstantFoldingTransformation
import org.jetbrains.research.ml.ast.transformations.deadcode.DeadCodeRemovalTransformation
import org.jetbrains.research.ml.ast.transformations.ifRedundantLinesRemoval.IfRedundantLinesRemovalTransformation
import org.jetbrains.research.ml.ast.transformations.multipleOperatorComparison.MultipleOperatorComparisonTransformation
import org.jetbrains.research.ml.ast.transformations.multipleTargetAssignment.MultipleTargetAssignmentTransformation
import org.jetbrains.research.ml.ast.transformations.outerNotElimination.OuterNotEliminationTransformation
import org.jetbrains.research.ml.ast.transformations.util.TransformationsWithSdkTest
import org.jetbrains.research.ml.ast.util.PsiFileHandler
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
            AugmentedAssignmentTransformation,
            DeadCodeRemovalTransformation,
            ConstantFoldingTransformation,
            MultipleOperatorComparisonTransformation,
            MultipleTargetAssignmentTransformation,
            IfRedundantLinesRemovalTransformation,
            ComparisonUnificationTransformation,
            OuterNotEliminationTransformation
        )
        val psiHandler = PsiFileHandler(myFixture, project)

        val inPsiFile = psiHandler.getPsiFile(inFile!!)
        val outPsiFile = psiHandler.getPsiFile(outFile!!)
        val commandStorage = PerformedCommandStorage(inPsiFile)

        lateinit var actualAfterForwardTransformations: String
        lateinit var actualAfterForwardTransformationsFile: String
        lateinit var actualAfterBackwardTransformations: String
        val expectedAfterForwardTransformations = outPsiFile.text
        val expectedAfterBackwardTransformations = inPsiFile.text

        ApplicationManager.getApplication().invokeAndWait {
            transformations.forEach { it.forwardApply(inPsiFile, commandStorage) }
            actualAfterForwardTransformations = inPsiFile.text
            commitPsiFile(inPsiFile)
            actualAfterForwardTransformationsFile = inPsiFile.containingFile.virtualFile.contentsToByteArray().toString(
                Charset.defaultCharset())

            TestCase.assertEquals(actualAfterForwardTransformations, actualAfterForwardTransformationsFile)

            val psiAfterBackwardTransformations = commandStorage.undoPerformedCommands()
            psiHandler.formatPsiFile(psiAfterBackwardTransformations)
            actualAfterBackwardTransformations = psiAfterBackwardTransformations.text
        }
        assertEquals(expectedAfterForwardTransformations, actualAfterForwardTransformations)
        assertEquals(expectedAfterBackwardTransformations, actualAfterBackwardTransformations)
    }

    private fun commitPsiFile(psiFile: PsiFile) {
        refresh(psiFile)
        val documentManager = PsiDocumentManager.getInstance(psiFile.project)
        val document = psiFile.viewProvider.document ?: error("No document found for $psiFile")
        documentManager.commitDocument(document)
        documentManager.doPostponedOperationsAndUnblockDocument(document)
//       not sure should i refresh it before or after committing so let's do both
        refresh(psiFile)
    }


//    Yep a lot of attempts to refresh it
    private fun refresh(psiFile: PsiFile) {
        LocalFileSystem.getInstance().refreshFiles(listOf(psiFile.virtualFile))
        VfsUtil.findFile(File(psiFile.virtualFile.path).toPath(), true)
        psiFile.virtualFile.refresh(true, true)
        VfsUtil.findFileByIoFile(File(psiFile.virtualFile.path), true)
        VfsUtil.markDirtyAndRefresh(false, true, true, psiFile.virtualFile)
        CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(psiFile, false)
    }
}
