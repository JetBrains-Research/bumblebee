package org.jetbrains.research.ml.ast.util

import java.io.File

enum class Extension(val value: String) {
    Py(".py"), Xml(".xml")
}

class TestFile(private val prefix: String, private val extension: Extension, val type: Type) {
    enum class Type {
        Input, Output
    }

    data class FileInfo(val file: File, val type: Type, val number: Number)

    fun check(file: File): FileInfo? {
        val number = "(?<=($prefix)_)\\d+(?=_.*${extension.value})".toRegex().find(file.name)?.value?.toInt()
        return number?.let { FileInfo(file, type, number) }
    }

    fun match(fileInfo: FileInfo): Boolean {
        return fileInfo.type == type
    }
}

object FileTestUtil {

    val File.content: String
        get() = this.readText().removeSuffix("\n")

    /**
     * We assume the format of the test files will be:
     *
     * inPrefix_i_anySuffix.inExtension
     * outPrefix_i_anySuffix.outExtension,
     *
     * where:
     * inPrefix and outPrefix are set in [inFile] and [outFile] together with extensions,
     * i is a number; two corresponding input and output files should have the same number,
     * suffixes can by any symbols not necessary the same for the corresponding files.
     */
    fun getInAndOutFilesMap(
        folder: String,
        inFile: TestFile = TestFile("in", Extension.Py, TestFile.Type.Input),
        outFile: TestFile = TestFile("out", Extension.Py, TestFile.Type.Output)
    ): Map<File, File> {
        val (files, folders) = File(folder).listFiles().orEmpty().partition { it.isFile }
        val inAndOutFilesGrouped = files.mapNotNull { inFile.check(it) ?: outFile.check(it) }.groupBy { it.number }
        val inAndOutFilesMap = inAndOutFilesGrouped.map { (number, fileInfoList) ->
            require(fileInfoList.size == 2) { "There are less or more than 2 test files with number $number" }
            val (f1, f2) = fileInfoList.sortedBy { it.type }.zipWithNext().first()
            require(inFile.match(f1) && outFile.match(f2)) { "Test files aren't paired with each other" }
            f1.file to f2.file
        }.sortedBy { it.first.name }.toMap()
//      Process all other nested files
        return folders.sortedBy { it.name }.map { getInAndOutFilesMap(it.absolutePath, inFile, outFile) }
            .fold(inAndOutFilesMap, { a, e -> a.plus(e) })
    }
}
