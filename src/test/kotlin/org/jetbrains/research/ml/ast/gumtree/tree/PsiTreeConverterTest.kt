package org.jetbrains.research.ml.ast.gumtree.tree

import com.github.gumtreediff.io.TreeIoUtils
import com.github.gumtreediff.tree.TreeContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiFile
import junit.framework.TestCase
import org.jetbrains.research.ml.ast.util.Extension
import org.jetbrains.research.ml.ast.util.FileTestUtil.content
import org.jetbrains.research.ml.ast.util.FileTestUtil.getInAndOutFilesMap
import org.jetbrains.research.ml.ast.util.ParametrizedBaseTest
import org.jetbrains.research.ml.ast.util.PsiTestUtil.equalTreeStructure
import org.jetbrains.research.ml.ast.util.TestFile
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class PsiTreeConverterTest : ParametrizedBaseTest(getResourcesRootPath(::PsiTreeConverterTest)) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}-{2}: ({0}, {1})")
        fun getTestData(): List<Array<Any>> {
            val files = getInAndOutFilesMap(
                getResourcesRootPath(::PsiTreeConverterTest),
                outFile = TestFile("out", Extension.Xml, TestFile.Type.Output)
            )
            val numberings = listOf(PreOrderNumbering, PostOrderNumbering)
            return files.flatMap { f -> numberings.map { n -> arrayOf(f.key, f.value, n) } }.toList()
        }
    }

    @JvmField
    @Parameterized.Parameter(0)
    var inSourceFile: File? = null

    @JvmField
    @Parameterized.Parameter(1)
    var outXmlFile: File? = null

    @JvmField
    @Parameterized.Parameter(2)
    var numbering: Numbering? = null

    @Test
    fun `compare tree structure test`() {
        val inFilePsi = getInFilePsi()
        TestCase.assertTrue(inFilePsi.equalTreeStructure(getInTreeContext(inFilePsi)))
    }

    @Test
    fun `compare xml test`() {
        val inContext = getInTreeContext(getInFilePsi())
        val expectedXml = outXmlFile!!.content
        val actualXml = TreeIoUtils.toXml(inContext).toString().removeSuffix("\n")
        TestCase.assertEquals(actualXml, expectedXml)
    }

    private fun getInFilePsi(): PsiFile {
        return myFixture.configureByFile(inSourceFile!!.absolutePath.replace(testDataPath, ""))
    }

    private fun getInTreeContext(psiFile: PsiFile): TreeContext {
        return ApplicationManager.getApplication().runReadAction<TreeContext> {
            PsiTreeConverter.convertTree(psiFile, numbering!!)
        }
    }
}
