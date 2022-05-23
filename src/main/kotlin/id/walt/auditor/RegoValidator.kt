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
        val validationResultJson = runBlocking {
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

        return JsonPath.parse(validationResultJson)?.read("$.result[0].expressions[0].value.allow") ?: false
    }
}
