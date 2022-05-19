package id.walt.auditor

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import java.io.StringReader
import java.lang.StringBuilder
import java.net.URL

object RegoValidator {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }


    fun validate(jsonInput: String, data: Map<String, Any?>, regoUrl: URL): Boolean {
        val input: Map<String, Any?> = Parser.default().parse(StringBuilder(jsonInput)) as JsonObject

        return validate(input, data, regoUrl)
    }


    fun validate(input: Map<String, Any?>, data: Map<String, Any?>, regoUrl: URL): Boolean {
        val rego = runBlocking {
            client.get(regoUrl).bodyAsText()
        }
        return validate(input, data, rego)
    }

    fun validate(input: Map<String, Any?>, data: Map<String, Any?>, rego: String): Boolean {
        val validationResultText = runBlocking {
            client.post("https://play.openpolicyagent.org/v1/data") {
                setBody(
                    JsonObject(
                        mapOf(
                            "data" to data,
                            "input" to input,
                            "rego_modules" to mapOf("policy.rego" to rego),
                            "strict" to true
                        )
                    ).toJsonString().also {
                        println("rego payload: $it")
                    }
                )
            }.bodyAsText()
        }
        val validationResult = Klaxon().parseJsonObject(StringReader(validationResultText))
        return (((((validationResult["result"] as JsonArray<*>)[0] as JsonObject)["expressions"] as JsonArray<*>)[0] as JsonObject)["value"] as JsonObject)["allow"] as Boolean
    }
}
