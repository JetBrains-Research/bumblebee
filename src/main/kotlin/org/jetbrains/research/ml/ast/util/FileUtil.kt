package org.jetbrains.research.ml.ast.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.io.FileUtil
import java.io.File

fun getContentFromFile(filePath: String): String = File(filePath).readText(Charsets.UTF_8)

fun createFolder(path: String) {
    val file = File(path)
    if (file.exists() && file.isFile) {
        file.delete()
    }
    if (!file.exists()) {
        file.mkdirs()
    }
}

fun createFile(path: String, content: String = ""): File {
    val file = File(path)
    ApplicationManager.getApplication().invokeAndWait {
        ApplicationManager.getApplication().runWriteAction {
            FileUtil.createIfDoesntExist(file)
            file.writeText(content)
        }
    }
    return file
}

fun getFilesFormFolder(path: String) = File(path).listFiles().orEmpty().filter { it.isFile }
