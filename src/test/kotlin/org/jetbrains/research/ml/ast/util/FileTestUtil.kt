package org.jetbrains.research.ml.ast.util

import java.io.File

enum class Extension(val value: String) {
    Py(".py"), Xml(".xml")
}

enum class Type {
    Input, Output
}

class TestFileFormat(private val prefix: String, private val extension: Extension, val type: Type) {
    data class TestFile(val file: File, val type: Type, val number: Number)

    fun check(file: File): TestFile? {
        val number = "(?<=${prefix}_)\\d+(?=(_.*)?${extension.value})".toRegex().find(file.name)?.value?.toInt()
        return number?.let { TestFile(file, type, number) }
    }

    fun match(testFile: TestFile): Boolean {
        return testFile.type == type
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
     * inPrefix and outPrefix are set in [inFormat] and [outFormat] together with extensions,
     * i is a number; two corresponding input and output files should have the same number,
     * suffixes can by any symbols not necessary the same for the corresponding files.
     */
    fun getInAndOutFilesMap(
        folder: String,
        inFormat: TestFileFormat = TestFileFormat("in", Extension.Py, Type.Input),
        outFormat: TestFileFormat = TestFileFormat("out", Extension.Py, Type.Output)
    ): Map<File, File> {
        val (files, folders) = File(folder).listFiles().orEmpty().partition { it.isFile }
//      Process files in the given folder
        val inAndOutFilesGrouped = files.mapNotNull { inFormat.check(it) ?: outFormat.check(it) }.groupBy { it.number }
        val inAndOutFilesMap = inAndOutFilesGrouped.map { (number, fileInfoList) ->
            require(fileInfoList.size == 2) { "There are less or more than 2 test files with number $number" }
            val (f1, f2) = fileInfoList.sortedBy { it.type }.zipWithNext().first()
            require(inFormat.match(f1) && outFormat.match(f2)) { "Test files aren't paired with each other" }
            f1.file to f2.file
        }.sortedBy { it.first.name }.toMap()
//      Process all other nested files
        return folders.sortedBy { it.name }.map { getInAndOutFilesMap(it.absolutePath, inFormat, outFormat) }
            .fold(inAndOutFilesMap, { a, e -> a.plus(e) })
    }

    // TODO: can we move some code into a common function?
    fun getTestFiles(
        folder: String,
        format: TestFileFormat = TestFileFormat("in", Extension.Py, Type.Input)
    ): List<File> {
        val (files, folders) = File(folder).listFiles().orEmpty().partition { it.isFile }
        val filesByFormat = files.mapNotNull { format.check(it)?.file }
        return folders.sortedBy { it.name }.map { getTestFiles(it.absolutePath, format) }
            .fold(filesByFormat, { a, e -> a.plus(e) })
    }
}
