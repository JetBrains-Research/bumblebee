package org.jetbrains.research.ml.ast.util

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkAdditionalData
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.projectRoots.impl.MockSdk
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.MultiMap
import com.jetbrains.python.PyNames
import com.jetbrains.python.codeInsight.typing.PyTypeShed.directory
import com.jetbrains.python.codeInsight.typing.PyTypeShed.findRootsForLanguageLevel
import com.jetbrains.python.codeInsight.userSkeletons.PyUserSkeletonsUtil
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.sdk.PythonSdkUtil
import org.jdom.Element
import java.io.File
import java.util.function.Consumer

/*
 The copy of https://github.com/JetBrains/intellij-community/blob/master/python/testSrc/com/jetbrains/python/PythonMockSdk.java
* */
class PythonMockSdk(private val getTestDataPath: String) {
    private val MOCK_SDK_NAME = "Mock Python SDK"

    fun create(version: String, vararg additionalRoots: VirtualFile): Sdk {
        val mockPath = "$getTestDataPath/MockSdk$version/"
        val sdkHome = File(mockPath, "bin/python$version").path
        val roots = MultiMap.create<OrderRootType, VirtualFile>()
        val classes = OrderRootType.CLASSES
        ContainerUtil.putIfNotNull(
            classes,
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(mockPath, "Lib")),
            roots
        )
        ContainerUtil.putIfNotNull(classes, PyUserSkeletonsUtil.getUserSkeletonsDirectory(), roots)
        val level = LanguageLevel.fromPythonVersion(version)
        val typeShedDir = directory!!
        findRootsForLanguageLevel(level!!)
            .forEach(
                Consumer { path: String? ->
                    ContainerUtil.putIfNotNull(
                        classes,
                        typeShedDir.findFileByRelativePath(
                            path!!
                        ),
                        roots
                    )
                }
            )
        val mockStubsPath = mockPath + PythonSdkUtil.SKELETON_DIR_NAME
        ContainerUtil.putIfNotNull(
            classes,
            LocalFileSystem.getInstance().refreshAndFindFileByPath(mockStubsPath),
            roots
        )
        roots.putValues(classes, listOf(*additionalRoots))
        val sdk = MockSdk(
            "$MOCK_SDK_NAME $version",
            sdkHome,
            "Python $version Mock SDK",
            roots,
            PyMockSdkType(version)
        )

        // com.jetbrains.python.psi.resolve.PythonSdkPathCache.getInstance() corrupts SDK, so have to clone
        return sdk.clone()
    }

    private class PyMockSdkType(string: String) : SdkTypeId {
        private val myVersionString: String = string
        private val mySdkIdName: String = PyNames.PYTHON_SDK_ID_NAME

        override fun getName(): String = mySdkIdName

        override fun getVersionString(sdk: Sdk): String? = myVersionString

        override fun saveAdditionalData(additionalData: SdkAdditionalData, additional: Element) {}

        override fun loadAdditionalData(currentSdk: Sdk, additional: Element): SdkAdditionalData? = null

        override fun isLocalSdk(sdk: Sdk): Boolean = false
    }
}
