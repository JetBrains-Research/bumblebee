package org.jetbrains.research.ml.ast.util

import java.io.File

object FileTestUtil {

    val File.content: String
        get() = this.readText().removeSuffix("\n")

    fun getInAndOutFilesMap(
        folder: String,
        oldExtension: String = ".py",
        newExtension: String = ".py"
    ): Map<File, File> {
        val inFileRegEx = "in_\\d*$oldExtension".toRegex()
        val inOutFileRegEx: Regex = "(in|out)_\\d*($oldExtension|$newExtension)".toRegex()
        val (inFiles, outFiles) = getNestedFiles(folder).toList().filter { inOutFileRegEx.containsMatchIn(it.name) }
            .partition { inFileRegEx.containsMatchIn(it.name) }
        if (inFiles.size != outFiles.size) {
            throw IllegalArgumentException(
                "Size of the list of input files does not equal size of the list of output files if the folder: $folder"
            )
        }
        return inFiles.associateWith { inFile ->
            // TODO: can I do it better?
            val outFileName = inFile.name.replace("in", "out").replace(oldExtension, newExtension)
            val outFile = File("${inFile.parent}/$outFileName")
            if (!outFile.exists()) {
                throw IllegalArgumentException("Out file $outFile does not exist!")
            }
            outFile
        }
    }

    private fun getNestedFiles(directoryName: String, files: MutableList<File> = ArrayList()): Sequence<File> {
        val root = File(directoryName)
        root.listFiles()?.forEach {
            if (it.isFile) {
                files.add(it)
            } else if (it.isDirectory) {
                getNestedFiles(it.absolutePath, files)
            }
        }
        return files.asSequence()
    }
}