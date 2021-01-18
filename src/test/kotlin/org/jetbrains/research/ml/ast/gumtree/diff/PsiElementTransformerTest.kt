package org.jetbrains.research.ml.ast.gumtree.diff

import com.intellij.openapi.command.WriteCommandAction
import org.jetbrains.research.ml.ast.gumtree.Util
import org.jetbrains.research.ml.ast.gumtree.tree.PostOrderNumbering
import org.jetbrains.research.ml.ast.util.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

// TODO: add more test cases
@RunWith(Parameterized::class)
class PsiElementTransformerTest : ParametrizedBaseTest(getResourcesRootPath(::PsiElementTransformerTest)) {
    private val numbering = PostOrderNumbering

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: ({0}, {1})")
        fun getTestData(): List<Array<Any>> {
            val files = FileTestUtil.getInAndOutFilesMap(
                getResourcesRootPath(::PsiElementTransformerTest),
                inFormat = TestFileFormat("src", Extension.Py, Type.Input),
                outFormat = TestFileFormat("dst", Extension.Py, Type.Output)
            )
            return files.map { f -> arrayOf(f.key, f.value) }
        }
    }

    @JvmField
    @Parameterized.Parameter(0)
    var srcFile: File? = null

    @JvmField
    @Parameterized.Parameter(1)
    var dstFile: File? = null

    @Test
    fun `apply src to dst actions`() = convertSrcToDst(srcFile!!, dstFile!!)

    @Test
    fun `apply dst to src actions`() = convertSrcToDst(dstFile!!, srcFile!!)

    // TODO: In the <fail> folder cases are stored which are incorrect even with GumTree trees
    //  and GumTree internal actions
    private fun inFailFolder(file: File): Boolean {
        val parent = file.parentFile
        if (parent.isDirectory && parent.nameWithoutExtension == "fail") {
            return true
        }
        return false
    }

    private fun deleteAllEmptyRows(str: String): String {
        return str.replace("(?m)^[ \t]*\r?\n".toRegex(), "").removeSuffix("\n")
    }

    private fun convertSrcToDst(srcFile: File, dstFile: File) {
        if (inFailFolder(srcFile)) {
            return
        }
        val srcPsi = myFixture.getPsiFile(srcFile)
        val dstPsi = myFixture.getPsiFile(dstFile)
        val expectedCode = dstPsi.text
        val srcContext = Util.getTreeContext(srcPsi, numbering)
        val dstContext = Util.getTreeContext(dstPsi, numbering)
        val matcher = Matcher(srcContext, dstContext)
        val actions = matcher.getEditActions()
        val w = PsiElementTransformer(project, srcPsi, dstPsi, numbering)
        WriteCommandAction.runWriteCommandAction(project) {
            w.applyActions(actions)
        }
        assertEquals(deleteAllEmptyRows(expectedCode), deleteAllEmptyRows(srcPsi.text))
    }
}
