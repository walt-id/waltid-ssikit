package id.walt.rest

import com.beust.klaxon.Klaxon
import com.nimbusds.jose.jwk.JWK
import id.walt.common.readWhenContent
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.W3CIssuer
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.KeyId
import id.walt.crypto.localTimeSecondsUtc
import id.walt.model.DidMethod
import id.walt.model.DidUrl
import id.walt.rest.core.*
import id.walt.rest.core.requests.did.EbsiCreateDidRequest
import id.walt.rest.core.requests.did.KeyCreateDidRequest
import id.walt.rest.core.requests.did.WebCreateDidRequest
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.did.DidWebCreateOptions
import id.walt.services.key.KeyFormat
import id.walt.services.key.KeyService
import id.walt.services.vc.JsonLdCredentialService
import id.walt.services.vc.VerificationResult
import id.walt.services.vc.VerificationType
import id.walt.signatory.ProofConfig
import id.walt.test.RESOURCES_PATH
import id.walt.test.getTemplate
import id.walt.test.readCredOffer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.StringUtils.countMatches
import java.io.File
import java.sql.Timestamp
import java.time.LocalDateTime

class CoreApiTest : AnnotationSpec() {

    init {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    }

    private val credentialService = JsonLdCredentialService.getService()
    val CORE_API_URL = "http://localhost:7013"
    val keyService = KeyService.getService()
    private val webOptions = DidWebCreateOptions("walt.id")

    val client = HttpClient() {
        install(ContentNegotiation) {
            json()
        }
        expectSuccess = false
    }

    fun get(path: String): HttpResponse = runBlocking {
        val response: HttpResponse = client.get("$CORE_API_URL$path") {
            accept(ContentType.Text.Html)
            headers {
                append(HttpHeaders.Authorization, "token")
            }
        }
        response.status.value shouldBe 200
        return@runBlocking response
    }

    fun post(path: String): HttpResponse = runBlocking {
        val response: HttpResponse = client.post("$CORE_API_URL$path") {
            accept(ContentType.Application.Json)
            headers {
                append(HttpHeaders.Authorization, "token")
            }
        }
        response.status.value shouldBe 200
        return@runBlocking response
    }

    inline fun <reified T> post(path: String): T = runBlocking {
        val response = client.post("$CORE_API_URL$path") {
            accept(ContentType.Application.Json)
            headers {
                append(HttpHeaders.Authorization, "token")
            }
        }.body<T>()
        //200 shouldBe response.status.value
        return@runBlocking response
    }

    @BeforeClass
    fun startServer() {
        CoreAPI.start(7013)
    }

    @AfterClass
    fun teardown() {
        CoreAPI.stop()
    }

    @BeforeEach
    fun beforeTest() {
        keyService.listKeys().forEach { keyService.delete(it.keyId.toString()) }
    }

    @Test
    fun testDocumentation() = runBlocking {
        val response = get("/v1/api-documentation").bodyAsText()

        response shouldContain "\"operationId\":\"health\""
        response shouldContain "Returns HTTP 200 in case all services are up and running"
    }

    @Test
    fun testHealth() = runBlocking {
        val response = get("/health")
        "OK" shouldBe response.bodyAsText()
    }

    @Test
    fun testGenKeyEd25519() = runBlocking {
        val keyId = client.post("$CORE_API_URL/v1/key/gen") {
            contentType(ContentType.Application.Json)
            setBody(GenKeyRequest(KeyAlgorithm.EdDSA_Ed25519))
        }.body<KeyId>()

        (keyId.id.length == 32) shouldBe true
    }

    @Test
    fun testGenKeySecp256k1() = runBlocking {
        val keyId = client.post("$CORE_API_URL/v1/key/gen") {
            contentType(ContentType.Application.Json)
            setBody(GenKeyRequest(KeyAlgorithm.ECDSA_Secp256k1))
        }.body<KeyId>()

        (keyId.id.length == 32) shouldBe true
    }

    @Test
    fun testGenKeyWrongParam() = runBlocking {
        val errorResp = client.post("$CORE_API_URL/v1/key/gen") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("keyAlgorithm" to "ECDSA_Secp256k1-asdf"))
        }
        println(errorResp.bodyAsText())
        val error = Klaxon().parse<ErrorResponse>(errorResp.bodyAsText())!!
        error.status shouldBe 400
        error.title shouldContain "GenKeyRequest"
    }

    @Test
    fun testListKey() = runBlocking {
        val keyIds = client.get("$CORE_API_URL/v1/key").body<List<String>>()
        keyIds.forEach { keyId -> (keyId.length >= 32) shouldBe true }
    }

    @Test
    fun `test export public key Secp256k1`() = runBlocking {
        val keyId = client.post("$CORE_API_URL/v1/key/gen") {
            contentType(ContentType.Application.Json)
            setBody(GenKeyRequest(KeyAlgorithm.ECDSA_Secp256k1))
        }.body<KeyId>()

        val key = client.post("$CORE_API_URL/v1/key/export") {
            contentType(ContentType.Application.Json)
            setBody(ExportKeyRequest(keyId.id, KeyFormat.JWK))
        }.bodyAsText()
        JWK.parse(key).isPrivate shouldBe false
    }

    @Test
    fun `test export private key Secp256k1`() = runBlocking {
        val keyId = client.post("$CORE_API_URL/v1/key/gen") {
            contentType(ContentType.Application.Json)
            setBody(GenKeyRequest(KeyAlgorithm.ECDSA_Secp256k1))
        }.body<KeyId>()

        val key = client.post("$CORE_API_URL/v1/key/export") {
            contentType(ContentType.Application.Json)
            setBody(ExportKeyRequest(keyId.id, KeyFormat.JWK, true))
        }.bodyAsText()
        (JWK.parse(key).isPrivate) shouldBe true
    }

    @Test
    fun `test export public key Ed25519`() = runBlocking {
        val keyId = client.post("$CORE_API_URL/v1/key/gen") {
            contentType(ContentType.Application.Json)
            setBody(GenKeyRequest(KeyAlgorithm.EdDSA_Ed25519))
        }.body<KeyId>()

        val key = client.post("$CORE_API_URL/v1/key/export") {
            contentType(ContentType.Application.Json)
            setBody(ExportKeyRequest(keyId.id, KeyFormat.JWK))
        }.bodyAsText()
        JWK.parse(key).isPrivate shouldBe false
    }

    @Test
    fun `test export private key Ed25519`() = runBlocking {
        val keyId = client.post("$CORE_API_URL/v1/key/gen") {
            contentType(ContentType.Application.Json)
            setBody(GenKeyRequest(KeyAlgorithm.EdDSA_Ed25519))
        }.body<KeyId>()

        val key = client.post("$CORE_API_URL/v1/key/export") {
            contentType(ContentType.Application.Json)
            setBody(ExportKeyRequest(keyId.id, KeyFormat.JWK, true))
        }.bodyAsText()
        (JWK.parse(key).isPrivate) shouldBe true
    }

    @Test
    fun `test import public key Secp256k1`() = runBlocking {

        val keyId = client.post("$CORE_API_URL/v1/key/import") {
            setBody(readWhenContent(File("src/test/resources/key/pubKeySecp256k1Jwk.json")))
        }.body<KeyId>()

        val key = keyService.load(keyId.id)

        key.keyId shouldBe keyId

        keyService.delete(keyId.id)

    }

    @Test
    fun `test import private key Secp256k1`() = runBlocking {

        val keyId = client.post("$CORE_API_URL/v1/key/import") {
            setBody(readWhenContent(File("src/test/resources/key/privKeySecp256k1Jwk.json")))
        }.body<KeyId>()

        val key = keyService.load(keyId.id)

        key.keyId shouldBe keyId

        keyService.delete(keyId.id)

    }

    @Test
    fun `test import public key Ed25519`() = runBlocking {

        val keyId = client.post("$CORE_API_URL/v1/key/import") {
            setBody(readWhenContent(File("src/test/resources/cli/pubKeyEd25519Jwk.json")))
        }.body<KeyId>()

        val key = keyService.load(keyId.id)

        key.keyId shouldBe keyId

        keyService.delete(keyId.id)

    }

    @Test
    fun `test import private key Ed25519`() = runBlocking {

        val keyId = client.post("$CORE_API_URL/v1/key/import") {
            setBody(readWhenContent(File("src/test/resources/cli/privKeyEd25519Jwk.json")))
        }.body<KeyId>()

        val key = keyService.load(keyId.id)

        key.keyId shouldBe keyId

        keyService.delete(keyId.id)

    }

    @Test
    fun testDidCreateKey() = runBlocking {
        val did = client.post("$CORE_API_URL/v1/did/create") {
            contentType(ContentType.Application.Json)
            setBody(KeyCreateDidRequest())
        }.bodyAsText()
        val didUrl = DidUrl.from(did)
        DidMethod.key.name shouldBe didUrl.method
    }

    @Test
    fun testDidCreateWeb() = runBlocking {
        val did = client.post("$CORE_API_URL/v1/did/create") {
            contentType(ContentType.Application.Json)
            setBody(WebCreateDidRequest())
        }.bodyAsText()
        val didUrl = DidUrl.from(did)
        DidMethod.web.name shouldBe didUrl.method
    }

    @Test
    fun testDidImport() = runBlocking {
        val testDid = "did:key:z6MkrA4JMXgNWXEgQqYwSynWe7LVkj5kwgcCpLbvGLXjWXHD"

        client.post("$CORE_API_URL/v1/did/import") {
            setBody(testDid)
        }.bodyAsText()

        val newDid = DidService.load(testDid)
        println("New DID: ${newDid.id}")
        newDid.id shouldBe testDid

        val key = keyService.load(testDid)
        println(key.keyId)

        key.keyId.id.removePrefix("did:key:") shouldBe "${testDid.removePrefix("did:key:")}#${testDid.removePrefix("did:key:")}"
    }

    // @Test - not possible, since all DID methods are supported now
    fun testDidCreateMethodNotSupported() = runBlocking {
        val errorResp = client.post("$CORE_API_URL/v1/did/create") {
            contentType(ContentType.Application.Json)
            setBody(EbsiCreateDidRequest())
        }
        errorResp.status.value shouldBe 400
        val error = Klaxon().parse<ErrorResponse>(errorResp.bodyAsText())!!
        error.status shouldBe 400
        "DID method EBSI not supported" shouldBe error.title
    }

    @Test
    fun testDidCreateVc() = runBlocking {
        val didHolder = client.post("$CORE_API_URL/v1/did/create") {
            contentType(ContentType.Application.Json)
            setBody(WebCreateDidRequest())
        }.bodyAsText()
        val didIssuer = client.post("$CORE_API_URL/v1/did/create") {
            contentType(ContentType.Application.Json)
            setBody(WebCreateDidRequest())
        }.bodyAsText()

        val credOffer = readCredOffer("vc-offer-simple-example")

        val vc = client.post("$CORE_API_URL/v1/vc/create") {
            contentType(ContentType.Application.Json)
            setBody(CreateVcRequest(didIssuer, didHolder, credOffer))
        }.bodyAsText()
        println("Credential received: $vc")
        val vcDecoded = VerifiableCredential.fromString(vc)
        println("Credential decoded: $vcDecoded")
        val vcEncoded = vcDecoded.encode()
        println("Credential encoded: $vcEncoded")
    }

    @Test
    fun testDidDelete() {
        forAll(
            row(DidMethod.key, null, null),
            row(DidMethod.web, null, webOptions),
            row(DidMethod.ebsi, null, null),
            row(DidMethod.key, keyService.generate(KeyAlgorithm.ECDSA_Secp256k1).id, null),
            row(DidMethod.key, keyService.generate(KeyAlgorithm.EdDSA_Ed25519).id, null),
            row(DidMethod.key, keyService.generate(KeyAlgorithm.RSA).id, null),
            row(DidMethod.web, keyService.generate(KeyAlgorithm.ECDSA_Secp256k1).id, webOptions),
            row(DidMethod.web, keyService.generate(KeyAlgorithm.EdDSA_Ed25519).id, webOptions),
            row(DidMethod.web, keyService.generate(KeyAlgorithm.RSA).id, webOptions),
            row(DidMethod.ebsi, keyService.generate(KeyAlgorithm.ECDSA_Secp256k1).id, null),
            row(DidMethod.ebsi, keyService.generate(KeyAlgorithm.EdDSA_Ed25519).id, null),
            row(DidMethod.ebsi, keyService.generate(KeyAlgorithm.RSA).id, null),
        ) { method, key, options ->
            val did = DidService.create(method, key, options)
            val response = runBlocking { client.delete("$CORE_API_URL/v1/did/$did") }
            response.status shouldBe HttpStatusCode.OK
            shouldThrow<Exception> {
                DidService.load(did)
            }
        }
    }

    @Test
    fun testPresentVerifyVC() = runBlocking {
        val credOffer = getTemplate("europass")
        val issuerDid = DidService.create(DidMethod.web, options = DidWebCreateOptions("example.com"))
        val subjectDid = DidService.create(DidMethod.key)

        credOffer.id = Timestamp.valueOf(LocalDateTime.now()).time.toString()
        credOffer.issuer = W3CIssuer(issuerDid)
        credOffer.credentialSubject!!.id = subjectDid

        credOffer.issued = localTimeSecondsUtc()

        val vcReqEnc = credOffer.toJson()

        println("Credential request:\n$vcReqEnc")

        val vcStr = credentialService.sign(vcReqEnc, ProofConfig(issuerDid = issuerDid))
        println("OUR VC STR: $vcStr")
        val vc = vcStr.toVerifiableCredential()

        println("Credential generated: $vc")

        val vp = client.post("$CORE_API_URL/v1/vc/present") {
            contentType(ContentType.Application.Json)
            setBody(PresentVcRequest(vcStr, subjectDid, "domain.com", "nonce"))
        }.bodyAsText()
        countMatches(vp, "\"proof\"") shouldBe 2

        val result = client.post("$CORE_API_URL/v1/vc/verify") {
            contentType(ContentType.Application.Json)
            setBody(VerifyVcRequest(vp))
        }.body<VerificationResult>()
        true shouldBe result.verified
        VerificationType.VERIFIABLE_PRESENTATION shouldBe result.verificationType
    }

    @Test
    fun testDeleteKey() = runBlocking {
        val kid = keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val response = client.delete("$CORE_API_URL/v1/key/delete") {
            setBody(kid.id)
        }
        response.status shouldBe HttpStatusCode.OK
        shouldThrow<Exception> {
            keyService.load(kid.id)
        }
    }

}
