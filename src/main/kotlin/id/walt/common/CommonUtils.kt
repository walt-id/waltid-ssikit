package id.walt.common

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import net.pwall.json.schema.JSONSchema
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Timestamp
import java.time.LocalDateTime


val client = HttpClient(CIO) {
  install(ContentNegotiation) {
    json()
  }
}

fun resolveContent(fileUrlContent: String): String {
  var file = File(fileUrlContent)
  if(file.exists()) {
    return file.readText()
  }
  if(Regex("^https?:\\/\\/.*$").matches(fileUrlContent)) {
    return runBlocking { client.get(fileUrlContent).bodyAsText() }
  }
  return fileUrlContent
}

fun resolveContentToFile(fileUrlContent: String, tempPrefix: String = "TEMP", tempPostfix: String = ".txt"): File {
  val fileCheck = File(fileUrlContent)
  if(fileCheck.exists())
    return fileCheck
  val file = File.createTempFile(tempPrefix, tempPostfix)
  file.writeText(resolveContent(fileUrlContent))
  return file
}

fun saveToFile(filepath: String, content: String, overwrite: Boolean = true) = File(filepath).let {
  if (overwrite) it.writeText(content) else it.appendText(content)
}
//  Files.newBufferedWriter(Paths.get(filepath), Charsets.UTF_8).use {
//    if (overwrite) it.write(content) else it.append(content)
//  }

fun validateForSchema(schema: String, data: String) = JSONSchema.parseFile(schema).validateBasic(data).valid