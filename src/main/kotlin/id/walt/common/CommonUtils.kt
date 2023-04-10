package id.walt.common

import id.walt.services.WaltIdServices.httpNoAuth
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import net.pwall.json.schema.JSONSchema
import java.io.File

fun resolveContent(fileUrlContent: String): String {
    val file = File(fileUrlContent)
    if (file.exists()) {
        return file.readText()
    }
    if (fileUrlContent.startsWith("class:")) {
        val enclosingClass = object {}.javaClass.enclosingClass
        val path = fileUrlContent.substring(6)
        var url = enclosingClass.getResource(path)
        if (url == null && !path.startsWith('/'))
            url = enclosingClass.getResource("/$path")
        return url?.readText() ?: fileUrlContent
    }
    if (Regex("^https?:\\/\\/.*$").matches(fileUrlContent)) {
        return runBlocking { httpNoAuth.get(fileUrlContent).bodyAsText() }
    }
    return fileUrlContent
}

fun resolveContentToFile(fileUrlContent: String, tempPrefix: String = "TEMP", tempPostfix: String = ".txt"): File {
    val fileCheck = File(fileUrlContent)
    if (!fileCheck.exists()) {
        File.createTempFile(tempPrefix, tempPostfix).let {
            it.writeText(resolveContent(fileUrlContent))
            return it
        }
    }
    return fileCheck
}

fun saveToFile(filepath: String, content: String, overwrite: Boolean = true) = File(filepath).let {
    if (overwrite) it.writeText(content) else it.appendText(content)
}
//  Files.newBufferedWriter(Paths.get(filepath), Charsets.UTF_8).use {
//    if (overwrite) it.write(content) else it.append(content)
//  }

fun validateForSchema(schema: String, data: String) = JSONSchema.parseFile(schema).validateBasic(data).valid

suspend inline fun <reified T> parseResponse(response: HttpResponse) = try {
    response.body<T>()
} catch (_: Exception) {
    throw Exception("Unexpected value: ${response.bodyAsText()}")
}

inline fun <reified T : Enum<T>> getEnumValue(strVal: String) = strVal.let {
    if (it.isEmpty()) {
        throw Exception("No ${T::class.java.name} defined")
    }
    enumValueOf<T>(it.lowercase())
}
