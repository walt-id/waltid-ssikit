package org.letstrust.rest

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertEquals

class CoreApiTest {

    val CORE_API_URL = "http://localhost:7000"

    val client = HttpClient(CIO)

    fun get(path: String): HttpResponse = runBlocking {
        val response: HttpResponse = client.get("$CORE_API_URL$path") {
            headers {
                append(HttpHeaders.Accept, "text/html")
                append(HttpHeaders.Authorization, "token")
            }
        }
        assertEquals(200, response.status.value)
        return@runBlocking response
    }

    fun post(path: String): HttpResponse = runBlocking {
        val response: HttpResponse = client.post("$CORE_API_URL$path") {
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.Authorization, "token")
            }
        }
        assertEquals(200, response.status.value)
        return@runBlocking response
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun startServer() {
            println("start")
            RestAPI.start()
        }

        @AfterClass
        @JvmStatic
        fun teardown() {
            RestAPI.stop()
        }
    }

    @Test
    fun testHealth() = runBlocking {
        val response = get("/health")
        assertEquals("OK", response.readText())
    }

    @Test
    fun testGenKey() = runBlocking {
        val response: HttpResponse = post("/v1/key/gen")
        val keyId = response.readText()
        assertEquals(48, keyId.length)
    }
}
