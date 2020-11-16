/*
 * Copyright (c) 2020.  Anastasiia Birillo, Elena Lyulina
 */

package org.jetbrains.research.ml.ast.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.UserDataHolderBase
import com.jetbrains.python.sdk.flavors.CondaEnvSdkFlavor
import org.apache.commons.lang3.SystemUtils
import org.jetbrains.research.ml.ast.util.Util.getCommandForInverseParser
import org.jetbrains.research.ml.ast.util.Util.runProcessBuilder
import org.junit.Ignore
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

@Ignore
open class ParametrizedBaseWithSdkTest(testDataRoot: String) : ParametrizedBaseTest(testDataRoot) {

    override fun mySetUp() {
        super.mySetUp()
        setupSdk()
    }

    protected fun setupSdk() {
        val project = myFixture.project
        val projectManager = ProjectRootManager.getInstance(project)
        val mySdkPath = CondaEnvSdkFlavor.getInstance().suggestHomePaths(myFixture.module, UserDataHolderBase())
        println("mySdkPath $mySdkPath")
        val sdkConfigurer = SdkConfigurer(project, projectManager)
        val sdkPath = ApplicationManager.getApplication().runReadAction<String> {
            runProcessBuilder(getCommandForInverseParser())
        }
        println("sdkPath $sdkPath")
        sdkConfigurer.setProjectSdk(sdkPath)
    }
}

object Util {
    /**
     * Represents a command passed to the [ProcessBuilder], where
     * [command] is a command to run (see [ProcessBuilder.command]),
     * [directory] is a working directory (see [ProcessBuilder.directory]),
     * and [environment] contains environment variables (see [ProcessBuilder.environment]).
     */
    data class Command(
        val command: List<String>,
        val directory: String? = null,
        val environment: Map<String, String>? = null
    )

    /*
     * Run ProcessBuilder and return output
     */
    fun runProcessBuilder(command: Command): String {
        val builder = ProcessBuilder(command.command)
        command.environment?.let {
            val environment = builder.environment()
            it.entries.forEach { e -> environment[e.key] = e.value }
        }
        command.directory?.let { builder.directory(File(it)) }
        builder.redirectErrorStream(true)
        val p = builder.start()
        return BufferedReader(InputStreamReader(p.inputStream)).readLines().joinToString(separator = "\n") { it }
    }

    /**
     * Runs inverse_parser_3 using python3, which it gets from [PYTHON3_PROPERTY] or sets the default one
     * To set [PYTHON3_PROPERTY], please, add -P[PYTHON3_PROPERTY]=/path/to/python3 to the command line
     */
    fun getCommandForInverseParser(): Command {
        val defaultPythonBin = if (SystemUtils.IS_OS_WINDOWS) "where python3" else "which python3"
        return Command(
            listOf("/bin/bash", "-c", defaultPythonBin)
        )
    }
}
