package id.walt.auditor

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import com.jayway.jsonpath.JsonPath
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.web3j.abi.datatypes.Bool
import java.io.File

object RegoValidator {
    private val log = KotlinLogging.logger {}
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }


    fun validate(jsonInput: String, data: Map<String, Any?>, rego: String, regoQuery: String): Boolean {
        val input: Map<String, Any?> = Parser.default().parse(StringBuilder(jsonInput)) as JsonObject

        return validate(input, data, rego, regoQuery)
    }

    const val TEMP_PREFIX = "_TEMP_"

    fun resolveRego(rego: String): File {
        var regoFile = File(rego)
        if(regoFile.exists()) {
            return regoFile
        }
        regoFile = File.createTempFile(TEMP_PREFIX, ".rego")
        regoFile.writeText(
            when(Regex("^https?:\\/\\/.*$").matches(rego)) {
                true -> runBlocking {
                    client.get(rego).bodyAsText()
                }
                else -> rego
            }
        )
        return regoFile
    }

    fun validate(input: Map<String, Any?>, data: Map<String, Any?>, regoPolicy: String, regoQuery: String): Boolean {
        val regoFile = resolveRego(regoPolicy)
        val dataFile = File.createTempFile("data", ".json")
        dataFile.writeText(JsonObject(data).toJsonString())
        try {
            val p = ProcessBuilder("opa", "eval", "-d", regoFile.absolutePath, "-d", dataFile.absolutePath, "-I", "-f", "values", regoQuery)
                .start()
            p.outputStream.writer().use { it.write(JsonObject(input).toJsonString()) }
            val output = p.inputStream.reader().use { it.readText() }
            p.waitFor()
            log.debug("rego eval output: {}", output)
            return Klaxon().parseArray<Boolean>(output)?.all { it } ?: false
        } finally {
            if(regoFile.exists() && regoFile.name.startsWith(TEMP_PREFIX)) {
              regoFile.delete()
            }
            if(dataFile.exists()) {
                dataFile.delete()
            }
        }
    }
}
