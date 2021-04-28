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
import kotlin.test.assertEquals

class EssifApiTest {

    val ESSIF_API_URL = "http://localhost:7002"

    val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun startServer() {
            RestAPI.startEssifApi(7002)
        }

        @AfterClass
        @JvmStatic
        fun teardown() {
            RestAPI.stopEssifApi()
        }
    }

    @Test
    fun testHealth() = runBlocking {
        val response = client.get<HttpResponse>("$ESSIF_API_URL/health")
        assertEquals("OK", response.readText())
    }

    @Test
    fun testOnboarding() = runBlocking {
        println("ESSIF onboarding of a Legal Entity by requesting a Verifiable ID")

        val credentialRequestUri = client.get<String>("$ESSIF_API_URL/v1/essif/ti/requestCredentialUri")
        println(credentialRequestUri)

        val didOwnershipReq = client.post<String>("$ESSIF_API_URL/v1/essif/ti/requestVerifiableCredential") {
            contentType(ContentType.Application.Json)
        }
        println(didOwnershipReq)

        val didOfLegalEntity = client.post<String>("$ESSIF_API_URL/v1/essif/enterprise/wallet/createDid") {
            contentType(ContentType.Application.Json)
        }
        println(didOfLegalEntity)

        val verifiableId = client.post<String>("$ESSIF_API_URL/v1/essif/enterprise/wallet/getVerifiableCredential") {
            contentType(ContentType.Application.Json)
            body = GetVcRequest("did:ebsi:234567", "did-ownership-req")
        }
        println(verifiableId)
    }
}
