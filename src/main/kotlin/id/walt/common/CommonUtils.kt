package id.walt.common

import id.walt.auditor.dynamic.OPAPolicyEngine
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import java.io.File


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
