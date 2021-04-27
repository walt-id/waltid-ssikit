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

class CoreApiTest {

    val CORE_API_URL = "http://localhost:7000"

    companion object {
        @BeforeClass
        @JvmStatic
        fun startServer() {
            println("start")
            RestAPI.start()
        }
        @AfterClass
        @JvmStatic fun teardown() {
            RestAPI.stop()
        }
    }

    @Test
    fun testDidCreate() = runBlocking<Unit> {
        val client = HttpClient(CIO)

        val response: HttpResponse = client.get("$CORE_API_URL/v1/health") {
            headers {
                append(HttpHeaders.Accept, "text/html")
                append(HttpHeaders.Authorization, "token")
            }
        }
        println(response)
    }

    @Test
    fun testVcCreate() {
        println("vc")
    }
}
