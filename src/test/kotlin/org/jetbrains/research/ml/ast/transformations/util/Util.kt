/*
 * Copyright (c) 2020.  Anastasiia Birillo, Elena Lyulina
 */

package org.jetbrains.research.ml.ast.transformations.util

import java.io.File
import java.lang.IllegalArgumentException

object Util {

    fun getContentFromFile(file: File) = file.readLines().joinToString(separator = "\n") { it }

    fun getInAndOutFilesMap(folder: String): Map<File, File> {
        val inFileRegEx = "in_\\d*.py".toRegex()
        val inOutFileRegEx = "(in|out)_\\d*.py".toRegex()
        val (inFiles, outFiles) = getNestedFiles(folder).toList().filter { inOutFileRegEx.containsMatchIn(it.name) }
                .partition { inFileRegEx.containsMatchIn(it.name) }
        if (inFiles.size != outFiles.size) {
            throw IllegalArgumentException("Size of the list of in files does not equal size of the list of out files if the folder: $folder")
        }
        return inFiles.associateWith { inFile ->
            val outFile = File("${inFile.parent}/${inFile.name.replace("in", "out")}")
            if (!outFile.exists()) {
                throw IllegalArgumentException("Out file $outFile does not exist!")
            }
            outFile
        }
    }

    private fun getNestedFiles(directoryName: String, files: MutableList<File> = ArrayList()): Sequence<File> {
        val root = File(directoryName)
        root.listFiles()?.let { fList ->
            for (file in fList) {
                if (file.isFile) {
                    files.add(file)
                } else if (file.isDirectory) {
                    getNestedFiles(file.absolutePath, files)
                }
            }
        }
        return files.asSequence()
    }
}

