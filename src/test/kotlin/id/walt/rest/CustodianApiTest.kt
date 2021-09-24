package id.walt.rest

import id.walt.rest.custodian.CustodianAPI
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

class CustodianApiTest : StringSpec({

    val CUSTODIAN_API_URL = "http://localhost:7013"

    val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
        expectSuccess = false
    }

    println("${CustodianAPI.DEFAULT_BIND_ADDRESS}/${CustodianAPI.DEFAULT_Custodian_API_PORT}")
    fun get(path: String): HttpResponse = runBlocking {
        val response: HttpResponse =
            client.get("http://${CustodianAPI.DEFAULT_BIND_ADDRESS}:${CustodianAPI.DEFAULT_Custodian_API_PORT}$path") {
                headers {
                    append(HttpHeaders.Accept, "text/html")
                    append(HttpHeaders.Authorization, "token")
                }
            }
        response.status.value shouldBe 200
        return@runBlocking response
    }

    "Starting Custodian API" {
        CustodianAPI.start()
    }

    /*"Test documentation" {
        val response = get("/v1/api-documentation").readText()

        response shouldContain "\"operationId\":\"health\""
        response shouldContain "Returns HTTP 200 in case all services are up and running"
    }*/
})
