package org.letstrust.rest

import id.walt.servicematrix.ServiceMatrix
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.lang3.StringUtils.countMatches
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.crypto.KeyId
import org.letstrust.model.DidMethod
import org.letstrust.model.DidUrl
import org.letstrust.model.VerifiableCredential
import org.letstrust.model.encodePretty
import org.letstrust.services.did.DidService
import org.letstrust.services.vc.VCService
import org.letstrust.services.vc.VerificationResult
import org.letstrust.services.vc.VerificationType
import org.letstrust.test.readCredOffer
import java.sql.Timestamp
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoreApiTest {

    init {
        ServiceMatrix("service-matrix.properties")
    }

    val credentialService = VCService.getService()
    val CORE_API_URL = "http://localhost:7003"

    val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
        expectSuccess = false
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
            RestAPI.startCoreApi(7003)
        }

        @AfterClass
        @JvmStatic
        fun teardown() {
            RestAPI.stopCoreApi()
        }
    }

    @Test
    fun testHealth() = runBlocking {
        val response = get("/health")
        assertEquals("OK", response.readText())
    }

    @Test
    fun testGenKeyEd25519() = runBlocking {

        val keyId = client.post<KeyId>("$CORE_API_URL/v1/key/gen") {
            contentType(ContentType.Application.Json)
            body = GenKeyRequest(KeyAlgorithm.EdDSA_Ed25519)
        }

        assertTrue(keyId.id.length > 45)
    }

    @Test
    fun testGenKeySecp256k1() = runBlocking {
        val keyId = client.post<KeyId>("$CORE_API_URL/v1/key/gen") {
            contentType(ContentType.Application.Json)
            body = GenKeyRequest(KeyAlgorithm.ECDSA_Secp256k1)
        }

        assertTrue(keyId.id.length > 45)
    }

    @Test
    fun testGenKeyWrongParam() = runBlocking {
        val errorResp = client.post<HttpResponse>("$CORE_API_URL/v1/key/gen") {
            contentType(ContentType.Application.Json)
            body = mapOf("keyAlgorithm" to "ECDSA_Secp256k1-asdf")
        }
        println(errorResp.readText())
        val error = Json.decodeFromString<ErrorResponse>(errorResp.readText())
        assertEquals(400, error.status)
        assertEquals("Couldn't deserialize body to GenKeyRequest", error.title)
    }


    @Test
    fun testListKey() = runBlocking {
        val keyIds = client.get<List<String>>("$CORE_API_URL/v1/key")
        keyIds.forEach { keyId -> assertTrue(keyId.length > 45) }
    }

    @Test
    fun testExportKey() = runBlocking {
        val keyId = client.post<KeyId>("$CORE_API_URL/v1/key/gen") {
            contentType(ContentType.Application.Json)
            body = GenKeyRequest(KeyAlgorithm.ECDSA_Secp256k1)
        }

        val key = client.post<String>("$CORE_API_URL/v1/key/export") {
            contentType(ContentType.Application.Json)
            body = ExportKeyRequest(keyId.id, "JWK")
        }
        assertTrue(key.length > 180)
    }

    @Test
    fun testDidCreateKey() = runBlocking {
        val did = client.post<String>("$CORE_API_URL/v1/did/create") {
            contentType(ContentType.Application.Json)
            body = CreateDidRequest(DidMethod.key)
        }
        val didUrl = DidUrl.from(did)
        assertEquals(DidMethod.key.name, didUrl.method)
    }

    @Test
    fun testDidCreateWeb() = runBlocking {
        val did = client.post<String>("$CORE_API_URL/v1/did/create") {
            contentType(ContentType.Application.Json)
            body = CreateDidRequest(DidMethod.web)
        }
        val didUrl = DidUrl.from(did)
        assertEquals(DidMethod.web.name, didUrl.method)
    }

    @Test
    fun testDidCreateMethodNotSupported() = runBlocking {
        val errorResp = client.post<HttpResponse>("$CORE_API_URL/v1/did/create") {
            contentType(ContentType.Application.Json)
            body = CreateDidRequest(DidMethod.ebsi)
        }
        assertEquals(400, errorResp.status.value)
        val error = Json.decodeFromString<ErrorResponse>(errorResp.readText())
        assertEquals(400, error.status)
        assertEquals("DID method EBSI not supported", error.title)
    }

    @Test
    fun testDidCreateVc() = runBlocking {
        val didHolder = client.post<String>("$CORE_API_URL/v1/did/create") {
            contentType(ContentType.Application.Json)
            body = CreateDidRequest(DidMethod.web)
        }
        val didIssuer = client.post<String>("$CORE_API_URL/v1/did/create") {
            contentType(ContentType.Application.Json)
            body = CreateDidRequest(DidMethod.web)
        }

        val credOffer = readCredOffer("vc-offer-simple-example")

        val vc = client.post<String>("$CORE_API_URL/v1/vc/create") {
            contentType(ContentType.Application.Json)
            body = CreateVcRequest(didIssuer, didHolder, credOffer)
        }
        println("Credential received: ${vc}")
        val vcDecoded = Json.decodeFromString<VerifiableCredential>(vc)
        println("Credential decoded: ${vcDecoded}")
        val vcEncoded = vcDecoded.encodePretty()
        println("Credential encoded: ${vcEncoded}")
    }

    @Test
    fun testPresentVerifyVC() = runBlocking {
        val credOffer = Json.decodeFromString<VerifiableCredential>(readCredOffer("vc-template-default"))
        val issuerDid = DidService.create(DidMethod.web)
        val subjectDid = DidService.create(DidMethod.key)

        credOffer.id = Timestamp.valueOf(LocalDateTime.now()).time.toString()
        credOffer.issuer = issuerDid
        credOffer.credentialSubject.id = subjectDid

        credOffer.issuanceDate = LocalDateTime.now()

        val vcReqEnc = Json { prettyPrint = true }.encodeToString(credOffer)

        println("Credential request:\n$vcReqEnc")

        val vcStr = credentialService.sign(issuerDid, vcReqEnc)
        val vc = Json.decodeFromString<VerifiableCredential>(vcStr)
        println("Credential generated: ${vc.encodePretty()}")

        val vp = client.post<String>("$CORE_API_URL/v1/vc/present") {
            contentType(ContentType.Application.Json)
            body = PresentVcRequest(vcStr, "domain.com", "nonce")
        }
        assertEquals(2, countMatches(vp, "proof"))

        val result = client.post<VerificationResult>("$CORE_API_URL/v1/vc/verify") {
            contentType(ContentType.Application.Json)
            body = VerifyVcRequest(vp)
        }
        assertEquals(true, result.verified)
        assertEquals(VerificationType.VERIFIABLE_PRESENTATION, result.verificationType)
    }

}
