package org.letstrust.rest

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.letstrust.crypto.KeyId
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoreApiTest {

    val CORE_API_URL = "http://localhost:7000"

    val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }

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

    inline fun <reified T> post(path: String): T = runBlocking {
        val response: T = client.post<T>("$CORE_API_URL$path") {
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.Authorization, "token")
            }
        }
        //assertEquals(200, response.status.value)
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
        val keyId = post<KeyId>("/v1/key/gen")
        assertTrue(keyId.id.length > 45)
    }

    @Test
    fun testListKey() = runBlocking {
        val keyIds = client.get<List<String>>("$CORE_API_URL/v1/key/list")
        keyIds.forEach { keyId -> assertTrue(keyId.length > 45) }
    }

    @Test
    fun testExportKey() = runBlocking {
        val keyId = post<KeyId>("/v1/key/gen")

        val key = client.post<String>("$CORE_API_URL/v1/key/export") {
            contentType(ContentType.Application.Json)
            body = ExportKeyRequest(keyId.id, "JWK")
        }
        assertTrue(key.length > 180)
    }
}
