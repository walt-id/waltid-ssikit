package id.walt.common

import id.walt.crypto.decBase64
import id.walt.crypto.decBase64Str
import id.walt.crypto.encBase64
import id.walt.crypto.encBase64Str
import id.walt.services.WaltIdServices.httpNoAuth
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.zip.*

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

fun compressGzip(data: ByteArray): ByteArray {
    val result = ByteArrayOutputStream()
    GZIPOutputStream(result).use {
        it.write(data)
    }
    return result.toByteArray()
}

fun uncompressGzip(data: ByteArray, idx: ULong? = null) =
    GZIPInputStream(data.inputStream()).bufferedReader().use {
        idx?.let { index ->
            var int = it.read()
            var count = 0U
            var char = int.toChar()
            while (int != -1 && count++ <= index) {
                char = int.toChar()
                int = it.read()
            }
            char
        }?.let {
            val array = CharArray(1)
            array[0] = it
            array
        } ?: it.readText().toCharArray()
    }

fun main(){
    val bitstring = "000001100000"
    println("bitstring: $bitstring")
    val compressed = compressGzip(bitstring.toByteArray())
    println("compressed: $compressed")
    val encoded = Base64.getEncoder().encode(compressed)
    println("encoded: ${encoded.decodeToString()}")
    val decoded = Base64.getDecoder().decode(encoded)
    println("decoded: ${decoded.decodeToString()}}")
    val revocationList = uncompressGzip(decoded, 7U)
    println("uncompressed: ${String(revocationList)}")
}
