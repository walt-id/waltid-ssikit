package id.walt.auditor

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.jayway.jsonpath.JsonPath
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.URL

object RegoValidator {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }


    fun validate(jsonInput: String, data: Map<String, Any?>, rego: String, resultPath: String): Boolean {
        val input: Map<String, Any?> = Parser.default().parse(StringBuilder(jsonInput)) as JsonObject

        return validate(input, data, rego, resultPath)
    }


    fun resolveRego(rego: String): String {
        val isHttpUrl = Regex("^https?:\\/\\/.*$").matches(rego)
        val isFile = !isHttpUrl && File(rego).exists()
        if(isHttpUrl) {
            return runBlocking {
                client.get(rego).bodyAsText()
            }
        } else if(isFile) {
            return File(rego).readText()
        } else {
            return rego
        }
    }

    fun validate(input: Map<String, Any?>, data: Map<String, Any?>, rego: String, resultPath: String): Boolean {
        val validationResultJson = runBlocking {
            client.post("https://play.openpolicyagent.org/v1/data") {
                setBody(
                    JsonObject(
                        mapOf(
                            "data" to data,
                            "input" to input,
                            "rego_modules" to mapOf("policy.rego" to resolveRego(rego)),
                            "strict" to true
                        )
                    ).toJsonString().also {
                        println("rego payload: $it")
                    }
                )
            }.bodyAsText()
        }

        return JsonPath.parse(validationResultJson)?.read(resultPath) ?: false
    }
}
