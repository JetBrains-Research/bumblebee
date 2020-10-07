package org.jetbrains.research.ml.coding.assistant

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity


class InitActivity : StartupActivity {
    private val logger: Logger = Logger.getInstance(javaClass)

    init {
        logger.info("${Plugin.PLUGIN_NAME}: startup activity")
    }

    override fun runActivity(project: Project) {
        logger.info("${Plugin.PLUGIN_NAME}: run activity")
    }
}
