package org.jetbrains.research.ml.ast.util.sdk

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.jetbrains.python.configuration.PyConfigurableInterpreterList
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.configuration.PyProjectVirtualEnvConfiguration
import org.apache.commons.lang3.SystemUtils
import java.io.BufferedReader
import java.io.InputStreamReader

private fun createBaseSdk(project: Project): Sdk {
    val myInterpreterList = PyConfigurableInterpreterList.getInstance(project)
    val myProjectSdksModel = myInterpreterList.model
    val pySdkType = PythonSdkType.getInstance()
    return myProjectSdksModel.createSdk(pySdkType, getPythonPath())
}

private fun checkPythonPath(pythonPath: String, expectedVersion: Int): Boolean {
    val builder = ProcessBuilder(listOf(pythonPath, "--version"))
    builder.redirectErrorStream(true)
    val p = builder.start()
    val output = BufferedReader(InputStreamReader(p.inputStream)).readLines()
        .joinToString("\n").trim()
    return output.matches("Python\\s+${expectedVersion}\\..*".toRegex())
}

private fun getPythonPath(expectedVersion: Int = 3): String {
    val scanCommand = if (SystemUtils.IS_OS_WINDOWS) "where" else "which"
    val builder = ProcessBuilder(listOf(scanCommand, "python3", "python"))
    builder.redirectErrorStream(true)
    val p = builder.start()
    val paths = BufferedReader(InputStreamReader(p.inputStream)).readLines()
    for (path in paths) {
        if (checkPythonPath(path, expectedVersion)) {
            return path
        }
    }
    error("Python interpreter not found")
}

private fun createVirtualEnvSdk(project: Project, baseSdk: Sdk, venvRoot: String): Sdk {
    var sdk: Sdk? = null
    ApplicationManager.getApplication().invokeAndWait {
        sdk = PyProjectVirtualEnvConfiguration.createVirtualEnvSynchronously(
            baseSdk = baseSdk,
            existingSdks = listOf(baseSdk),
            venvRoot = venvRoot,
            projectBasePath = project.basePath,
            project = project,
            module = null
        )
    }
    return sdk ?: error("Internal error: SDK for ${project.name} project was not created")
}

fun setSdkToProject(project: Project, venvRoot: String) {
    val baseSdk = createBaseSdk(project)
    val sdk = createVirtualEnvSdk(project, baseSdk, venvRoot)

    val projectManager = ProjectRootManager.getInstance(project)
    val sdkConfigurer = SdkConfigurer(project, projectManager)
    sdkConfigurer.setProjectSdk(sdk)
}
