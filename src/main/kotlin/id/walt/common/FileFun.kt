package id.walt.common

import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.readText

fun readEssif(fileName: String) = ClassLoader.getSystemResource("essif/${fileName}.json").readText(Charsets.UTF_8)


fun readWhenContent(file: File, errorMessage: String? = null) = readWhenContent(file.toPath(), errorMessage)

fun readWhenContent(file: Path, errorMessage: String? = null) = when {
    file.exists() && file.fileSize() > 0 -> file.readText()
    else -> throw Exception(errorMessage ?: "Expecting file with content at: ${file.absolutePathString()}")
}

fun readBearerToken(tokenFile: File, errMsg: String? = null): String = readWhenContent(
    tokenFile,
    errMsg
).replace("\n", "")
