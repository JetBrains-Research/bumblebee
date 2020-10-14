package org.jetbrains.research.ml.ast.transformations.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyElementGenerator
import org.jetbrains.research.ml.ast.transformations.DeadCodeRemovalTransformation
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class DeadCodeRemovalTransformationTest : TransformationsTest(getResourcesRootPath(::TransformationsTest)) {
    private fun runTransformation(element: PsiElement, toStoreMetadata: Boolean) {
        val transformation = DeadCodeRemovalTransformation(
            project,
            PyElementGenerator.getInstance(project)
        )
        transformation.apply(element, toStoreMetadata)
    }
    @Test
    fun testBasicCase() {
        assertCodeTransformation(inFile!!, outFile!!, this::runTransformation)
    }
}