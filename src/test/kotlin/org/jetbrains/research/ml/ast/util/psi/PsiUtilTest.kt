package org.jetbrains.research.ml.ast.util.psi

import org.jetbrains.research.ml.ast.util.FileTestUtil.getTestFiles
import org.jetbrains.research.ml.ast.util.ParametrizedBaseWithSdkTest
import org.jetbrains.research.ml.ast.util.isCorrect
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class PsiUtilTest : ParametrizedBaseWithSdkTest(getResourcesRootPath(::PsiUtilTest)) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: ({0} : {1})")
        fun getTestData(): List<Array<Any>> {
            return getTestFiles(getResourcesRootPath(::PsiUtilTest, resourcesRoot))
                .map { arrayOf(it, isCorrect(it)) }
        }

        private fun isCorrect(file: File): Boolean {
            return file.nameWithoutExtension.endsWith("_correct")
        }
    }

    @JvmField
    @Parameterized.Parameter(0)
    var inFile: File? = null

    @JvmField
    @Parameterized.Parameter(1)
    var isCorrect: Boolean? = null

    @Test
    fun test() {
        val psiFile = myFixture.configureByFile(inFile!!.path)
        assertEquals(isCorrect!!, psiFile.isCorrect())
    }
}
