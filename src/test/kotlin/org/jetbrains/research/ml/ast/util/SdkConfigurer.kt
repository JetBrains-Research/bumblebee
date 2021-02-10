package org.jetbrains.research.ml.ast.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.pythonSdk
import com.jetbrains.python.statistics.modules
import java.util.logging.Logger

class SdkConfigurer(
    private val project: Project,
    private val projectManager: ProjectRootManager
) {
    private val logger = Logger.getLogger(javaClass.name)

    private fun connectSdkWithProject(sdk: Sdk) {
        logger.info("Connecting SDK with project files")
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                projectManager.projectSdk = sdk
            }
            project.pythonSdk = sdk
            project.modules.forEach { module ->
                module.pythonSdk = sdk
            }
        }
    }

    fun setProjectSdk(sdk: Sdk) {
        logger.info("Setting up SDK: $sdk for project $project")
        connectSdkWithProject(sdk)
        PythonSdkType.getInstance().setupSdkPaths(sdk)
    }
}
