package org.jetbrains.research.ml.ast.gumtree.tree

import com.github.gumtreediff.io.TreeIoUtils
import junit.framework.TestCase
import org.jetbrains.research.ml.ast.gumtree.Util.getTreeContext
import org.jetbrains.research.ml.ast.util.*
import org.jetbrains.research.ml.ast.util.FileTestUtil.content
import org.jetbrains.research.ml.ast.util.FileTestUtil.getInAndOutFilesMap
import org.jetbrains.research.ml.ast.util.PsiTestUtil.equalTreeStructure
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
                outFormat = TestFileFormat("out", Extension.Xml, Type.Output)
            )
            val numberings = listOf(PreOrderNumbering, PostOrderNumbering)
            return files.flatMap { f -> numberings.map { n -> arrayOf(f.key, f.value!!, n) } }.toList()
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
        val inFilePsi = myFixture.getPsiFile(inSourceFile!!)
        TestCase.assertTrue(inFilePsi.equalTreeStructure(getTreeContext(inFilePsi, numbering!!)))
    }

    @Test
    fun `compare xml test`() {
        val inContext = getTreeContext(myFixture.getPsiFile(inSourceFile!!), numbering!!)
        val expectedXml = outXmlFile!!.content
        // Note: the LE and GE operators are stored correctly (<=, >=), but the XML contains &lt;= and &gt;=
        // TODO: should we fix the formatter??
        val actualXml = TreeIoUtils.toXml(inContext).toString().removeSuffix("\n")
        TestCase.assertEquals(expectedXml, actualXml)
    }
}
