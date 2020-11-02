package org.jetbrains.research.ml.ast.util

import java.io.File

object FileTestUtil {

    val File.content: String
        get() = this.readText().removeSuffix("\n")

    fun getInAndOutFilesMap(
        folder: String,
        inExtension: String = ".py",
        outExtension: String = ".py"
    ): Map<File, File> {
        val inFileRegEx = "in_\\d*$inExtension".toRegex()
        val inOutFileRegEx: Regex = "(in|out)_\\d*($inExtension|$outExtension)".toRegex()
        val (inFiles, outFiles) = getNestedFiles(folder).toList().filter { inOutFileRegEx.containsMatchIn(it.name) }
            .partition { inFileRegEx.containsMatchIn(it.name) }
        if (inFiles.size != outFiles.size) {
            throw IllegalArgumentException(
                "Size of the list of input files does not equal size of the list of output files in the folder: $folder"
            )
        }
        return inFiles.sortedBy { it.name }.associateWith { inFile ->
            // TODO: can I do it better?
            val outFileName = inFile.name.replace("in", "out").replace(inExtension, outExtension)
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
