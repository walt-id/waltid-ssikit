package id.walt.common

import id.walt.services.WaltIdServices.http
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import java.io.File

fun resolveContent(fileUrlContent: String): String {
    val file = File(fileUrlContent)
    if (file.exists()) {
        return file.readText()
    }
    if (fileUrlContent.startsWith("class:")) {
        val clazz = object{}.javaClass.enclosingClass
        val path = fileUrlContent.substring(6)
        var url = clazz.getResource(path)
        if (url == null && !path.startsWith('/'))
            url = clazz.getResource("/$path")
        return url?.readText() ?: fileUrlContent
    }
    if (Regex("^https?:\\/\\/.*$").matches(fileUrlContent)) {
        return runBlocking { http.get(fileUrlContent).bodyAsText() }
    }
    return fileUrlContent
}

fun resolveContentToFile(fileUrlContent: String, tempPrefix: String = "TEMP", tempPostfix: String = ".txt"): File {
    val fileCheck = File(fileUrlContent)
    if (fileCheck.exists())
        return fileCheck
    val file = File.createTempFile(tempPrefix, tempPostfix)
    file.writeText(resolveContent(fileUrlContent))
    return file
}
