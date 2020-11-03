package org.jetbrains.research.ml.ast.util

import java.io.File

enum class Extension(val value: String) {
    Py(".py"), Xml(".xml")
}

object FileTestUtil {

    val File.content: String
        get() = this.readText().removeSuffix("\n")

    fun getInAndOutFilesMap(
        folder: String,
        inExt: Extension = Extension.Py,
        outExt: Extension = Extension.Py
    ): Map<File, File> {
        fun File.number(): Int = "(?<=(in|out)_)\\d+".toRegex().find(this.name)!!.value.toInt()
        fun File.isInFile(): Boolean = "in_\\d+.*${inExt.value}".toRegex().containsMatchIn(this.name)
        fun File.isOutFile(): Boolean = "out_\\d+.*${outExt.value}".toRegex().containsMatchIn(this.name)

        val (files, folders) = File(folder).listFiles().orEmpty().partition { it.isFile }
//      Process files in the given folder
        val inAndOutFiles = files.filter { it.isInFile() || it.isOutFile() }.groupBy { it.number() }
        val inAndOutFilesMap = inAndOutFiles.toSortedMap().map { (k, v) ->
            require(v.size == 2) { "There are less or more than 2 test files with number $k" }
            val (inFile, outFile) = v.sorted().zipWithNext().first()
            require(inFile.isInFile() && outFile.isOutFile()) { "Test files aren't paired with each other" }
            inFile to outFile
        }.toMap()
//      Process all other nested files
        return folders.sortedBy { it.name }.map { getInAndOutFilesMap(it.absolutePath, inExt, outExt) }
            .fold(inAndOutFilesMap, { a, e -> a.plus(e) })
    }
}
