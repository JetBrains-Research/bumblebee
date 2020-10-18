package org.jetbrains.research.ml.ast.transformations.deadcode

import com.intellij.psi.PsiElement
import org.jetbrains.research.ml.ast.transformations.DeadCodeRemovalTransformation
import org.jetbrains.research.ml.ast.transformations.util.TransformationsTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class DeadCodeRemovalTransformationTest : TransformationsTest(getResourcesRootPath(::TransformationsTest)) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: ({0}, {1})")
        fun getTestData() = getInAndOutArray(::DeadCodeRemovalTransformationTest).filter { it[0].name.contains("5") }
    }

    private fun runTransformation(element: PsiElement, toStoreMetadata: Boolean) {
        val transformation = DeadCodeRemovalTransformation()
        transformation.apply(element, toStoreMetadata)
    }

    @Test
    fun testTransformation() {
        assertCodeTransformation(inFile!!, outFile!!, this::runTransformation)
    }
}
