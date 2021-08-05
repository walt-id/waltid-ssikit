package id.walt.rest

import com.beust.klaxon.Klaxon
import com.nimbusds.jose.jwk.JWK
import id.walt.servicematrix.ServiceMatrix
import id.walt.vclib.VcLibManager
import id.walt.vclib.vclist.Europass
import id.walt.vclib.vclist.VerifiableAttestation
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.StringUtils.countMatches
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.KeyId
import id.walt.crypto.localTimeSecondsUtc
import id.walt.model.DidMethod
import id.walt.model.DidUrl
import id.walt.services.did.DidService
import id.walt.services.key.KeyFormat
import id.walt.services.vc.VCService
import id.walt.services.vc.VerificationResult
import id.walt.services.vc.VerificationType
import id.walt.test.getTemplate
import id.walt.test.readCredOffer
import id.walt.vclib.Helpers.encode
import id.walt.vclib.Helpers.toCredential
import java.io.File
import java.sql.Timestamp
import java.time.LocalDateTime

class CoreApiTest : AnnotationSpec() {

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
        200 shouldBe response.status.value
        return@runBlocking response
    }

    fun post(path: String): HttpResponse = runBlocking {
        val response: HttpResponse = client.post("$CORE_API_URL$path") {
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.Authorization, "token")
            }
        }
        200 shouldBe response.status.value
        return@runBlocking response
    }

    inline fun <reified T> post(path: String): T = runBlocking {
        val response: T = client.post<T>("$CORE_API_URL$path") {
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.Authorization, "token")
            }
        }
        //200 shouldBe response.status.value
        return@runBlocking response
    }

    @BeforeClass
    fun startServer() {
        RestAPI.startCoreApi(7003)
    }

    @AfterClass
    fun teardown() {
        RestAPI.stopCoreApi()
    }

    @Test
    fun testHealth() = runBlocking {
        val response = get("/health")
        "OK" shouldBe response.readText()
    }

    @Test
    fun testGenKeyEd25519() = runBlocking {

        val keyId = client.post<KeyId>("$CORE_API_URL/v1/key/gen") {
            contentType(ContentType.Application.Json)
            body = GenKeyRequest(KeyAlgorithm.EdDSA_Ed25519)
        }

        (keyId.id.length == 32) shouldBe true
    }

    @Test
    fun testGenKeySecp256k1() = runBlocking {
        val keyId = client.post<KeyId>("$CORE_API_URL/v1/key/gen") {
            contentType(ContentType.Application.Json)
            body = GenKeyRequest(KeyAlgorithm.ECDSA_Secp256k1)
        }

        (keyId.id.length == 32) shouldBe true
    }

    @Test
    fun testGenKeyWrongParam() = runBlocking {
        val errorResp = client.post<HttpResponse>("$CORE_API_URL/v1/key/gen") {
            contentType(ContentType.Application.Json)
            body = mapOf("keyAlgorithm" to "ECDSA_Secp256k1-asdf")
        }
        println(errorResp.readText())
        val error = Klaxon().parse<ErrorResponse>(errorResp.readText())!!
        400 shouldBe error.status
        "Couldn't deserialize body to GenKeyRequest" shouldBe error.title
    }

    @Test
    fun testListKey() = runBlocking {
        val keyIds = client.get<List<String>>("$CORE_API_URL/v1/key")
        keyIds.forEach { keyId -> (keyId.length >= 32) shouldBe true }
    }

    @Test
    fun testExportPublicKey() = runBlocking {
        val keyId = client.post<KeyId>("$CORE_API_URL/v1/key/gen") {
            contentType(ContentType.Application.Json)
            body = GenKeyRequest(KeyAlgorithm.ECDSA_Secp256k1)
        }

        val key = client.post<String>("$CORE_API_URL/v1/key/export") {
            contentType(ContentType.Application.Json)
            body = ExportKeyRequest(keyId.id, KeyFormat.JWK)
        }
        JWK.parse(key).isPrivate shouldBe false
    }

    @Test
    fun testExportPrivateKey() = runBlocking {
        val keyId = client.post<KeyId>("$CORE_API_URL/v1/key/gen") {
            contentType(ContentType.Application.Json)
            body = GenKeyRequest(KeyAlgorithm.EdDSA_Ed25519)
        }

        val key = client.post<String>("$CORE_API_URL/v1/key/export") {
            contentType(ContentType.Application.Json)
            body = ExportKeyRequest(keyId.id, KeyFormat.JWK, true)
        }
        (JWK.parse(key).isPrivate) shouldBe true
    }

    @Test
    fun testDidCreateKey() = runBlocking {
        val did = client.post<String>("$CORE_API_URL/v1/did/create") {
            contentType(ContentType.Application.Json)
            body = CreateDidRequest(DidMethod.key)
        }
        val didUrl = DidUrl.from(did)
        DidMethod.key.name shouldBe didUrl.method
    }

    @Test
    fun testDidCreateWeb() = runBlocking {
        val did = client.post<String>("$CORE_API_URL/v1/did/create") {
            contentType(ContentType.Application.Json)
            body = CreateDidRequest(DidMethod.web)
        }
        val didUrl = DidUrl.from(did)
        DidMethod.web.name shouldBe didUrl.method
    }

    // @Test - not possible, since all DID methods are supported now
    fun testDidCreateMethodNotSupported() = runBlocking {
        val errorResp = client.post<HttpResponse>("$CORE_API_URL/v1/did/create") {
            contentType(ContentType.Application.Json)
            body = CreateDidRequest(DidMethod.ebsi)
        }
        400 shouldBe errorResp.status.value
        val error = Klaxon().parse<ErrorResponse>(errorResp.readText())!!
        400 shouldBe error.status
        "DID method EBSI not supported" shouldBe error.title
    }


    @Test
    fun testGetVcDefaultTemplate() = runBlocking {

        val defaultTemplate = client.get<String>("$CORE_API_URL/v1/vc/templates/default") {
            contentType(ContentType.Application.Json)
        }
        val input = File("templates/vc-template-default.json").readText().replace("\\s".toRegex(), "")

        val vc = input.toCredential()
        val enc = Klaxon().toJsonString(vc as VerifiableAttestation)
        input shouldEqualJson enc

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
        println("Credential received: $vc")
        val vcDecoded = VcLibManager.getVerifiableCredential(vc)
        println("Credential decoded: $vcDecoded")
        val vcEncoded = vcDecoded.encode()
        println("Credential encoded: $vcEncoded")
    }

    @Test
    fun testPresentVerifyVC() = runBlocking {
        val credOffer = getTemplate("europass") as Europass
        val issuerDid = DidService.create(DidMethod.web)
        val subjectDid = DidService.create(DidMethod.key)

        credOffer.id = Timestamp.valueOf(LocalDateTime.now()).time.toString()
        credOffer.issuer = issuerDid
        credOffer.credentialSubject!!.id = subjectDid

        credOffer.issuanceDate = localTimeSecondsUtc()

        val vcReqEnc = Klaxon().toJsonString(credOffer)

        println("Credential request:\n$vcReqEnc")

        val vcStr = credentialService.sign(issuerDid, vcReqEnc)
        println("OUR VC STR: $vcStr")
        val vc = vcStr.toCredential()

        println("Credential generated: ${vc}")

        val vp = client.post<String>("$CORE_API_URL/v1/vc/present") {
            contentType(ContentType.Application.Json)
            body = PresentVcRequest(vcStr, "domain.com", "nonce")
        }
        countMatches(vp, "proof") shouldBe 2

        val result = client.post<VerificationResult>("$CORE_API_URL/v1/vc/verify") {
            contentType(ContentType.Application.Json)
            body = VerifyVcRequest(vp)
        }
        true shouldBe result.verified
        VerificationType.VERIFIABLE_PRESENTATION shouldBe result.verificationType
    }

}
