package org.jetbrains.research.ml.ast.transformations.inverse

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import org.jetbrains.research.ml.ast.transformations.MetaDataStorage
import org.jetbrains.research.ml.ast.transformations.util.TransformationsTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class InverseTransformationTest : TransformationsTest(getResourcesRootPath(::InverseTransformationTest)) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: ({0}, {1})")
        fun getTestData() = getInAndOutArray(::InverseTransformationTest)
    }

    @Test
//   Todo: rename
    fun `just run to test`() {
        val psiFile = getPsiFile(inFile!!)
        val metaDataStorage = MetaDataStorage(psiFile)
        ApplicationManager.getApplication().invokeAndWait {
            println(psiFile.text)
            WriteCommandAction.runWriteCommandAction(psiFile.project) {
                metaDataStorage.perform({ psiFile.children[4].delete() }, "Delete fifth child")
            }
            println(psiFile.text)
            metaDataStorage.undoCommands()
        }
    }
}
