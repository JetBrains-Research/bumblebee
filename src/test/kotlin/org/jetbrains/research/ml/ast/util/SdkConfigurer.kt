package org.jetbrains.research.ml.ast.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.jetbrains.python.sdk.PyDetectedSdk
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.pythonSdk
import com.jetbrains.python.statistics.modules
import java.util.logging.Logger

class SdkConfigurer(
    private val project: Project,
    private val projectManager: ProjectRootManager
) {
    private val LOG = Logger.getLogger(javaClass.name)

    private fun createSdk(sdkPath: String): Sdk {
        val sdk = PyDetectedSdk(sdkPath)
        LOG.info("Created SDK: $sdk")
        return sdk
    }

    private fun connectSdkWithProject(sdk: Sdk) {
        LOG.info("Connecting SDK with project files")
        val jdkTable = ProjectJdkTable.getInstance()
        val app = ApplicationManager.getApplication()
        println("${app.isDispatchThread} ${app.isWriteAccessAllowed}")
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                jdkTable.addJdk(sdk)
                projectManager.projectSdk = sdk
            }
            project.pythonSdk = sdk
            project.modules.forEach { module ->
                module.pythonSdk = sdk
            }
        }
    }

    fun setProjectSdk(sdkPath: String) {
        LOG.info("Setting up SDK for project $project")
        val sdk = createSdk(sdkPath)
        connectSdkWithProject(sdk)
        PythonSdkType.getInstance().setupSdkPaths(sdk)
    }
}
