package id.walt.auditor

import com.beust.klaxon.Klaxon
import id.walt.common.KlaxonWithConverters
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.WaltIdServices
import id.walt.services.did.DidService
import id.walt.signatory.ProofConfig
import id.walt.signatory.Signatory
import id.walt.test.RESOURCES_PATH
import id.walt.test.readVerifiableCredential
import id.walt.test.readVerifiablePresentation
import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneOffset


@Ignored // TODO: Ignored test since ebsi dids are currently not resolving
class AuditorApiTest : AnnotationSpec() {

    init {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    }

    val Auditor_HOST = "localhost"
    val Auditor_API_PORT = 7001
    val Auditor_API_URL = "http://$Auditor_HOST:$Auditor_API_PORT"

    val DEFAULT_POLICIES = "SignaturePolicy, JsonSchemaPolicy"

    val client = HttpClient() {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        expectSuccess = false
    }

    @BeforeClass
    fun startServer() {
        AuditorRestAPI.start(Auditor_API_PORT)
    }

    @AfterClass
    fun teardown() {
        AuditorRestAPI.stop()
    }

    @Test
    fun testHealth() = runBlocking {
        val response = client.get("$Auditor_API_URL/health").bodyAsText()
        response shouldBe "OK"
    }

    @Test
    fun testListVerificationPolicies() = runBlocking {
        val policies = client.get("$Auditor_API_URL/v1/policies") {
            contentType(ContentType.Application.Json)
        }.body<List<VerificationPolicyMetadata>>()

        policies.map { it.id } shouldContainAll listOf("SignaturePolicy", "JsonSchemaPolicy", "TrustedSchemaRegistryPolicy")
    }

    private fun postAndVerify(vcToVerify: String, policyList: String = DEFAULT_POLICIES) {
        val verificationResponseJson = runBlocking {
            WaltIdServices.httpNoAuth.post(URL("http", Auditor_HOST, Auditor_API_PORT, "/v1/verify")) {
                setBody(
                    KlaxonWithConverters().toJsonString(
                        VerificationRequest(
                            policies = policyList.split(",").map { p -> PolicyRequest(policy = p.trim()) }.toList(),
                            credentials = listOf(
                                vcToVerify.toVerifiableCredential()
                            )
                        )
                    )
                )
            }.bodyAsText()
        }

        println(verificationResponseJson)

        val vr = Klaxon().parse<VerificationResponse>(verificationResponseJson)!!
        vr.valid shouldBe true
    }

    @Test
    fun testCredentialVerification() = runBlocking {
        val signatory = Signatory.getService()
        val did = DidService.create(DidMethod.key)

        val vcToVerify = signatory.issue(
            "VerifiableId", ProofConfig(
                subjectDid = did, issuerDid = did, issueDate = LocalDateTime.of(2020, 11, 3, 0, 0).toInstant(ZoneOffset.UTC)
            )
        )

        postAndVerify(vcToVerify)
    }

    @Test
    fun testVerifiableAuthorizationCredential() {
        postAndVerify(
            readVerifiableCredential("VerifiableAuthorization"),
            "JsonSchemaPolicy, SignaturePolicy,TrustedSubjectDidPolicy,TrustedIssuerDidPolicy"
        )
    }

    @Test
    fun testVerifiableAttestationCredential() {
        postAndVerify(readVerifiableCredential("VerifiableAttestation"))
    }

    @Test
    fun testVerifiableDiploma() {
        postAndVerify(
            readVerifiableCredential("VerifiableDiploma"),
            "JsonSchemaPolicy,SignaturePolicy,TrustedSubjectDidPolicy,TrustedIssuerDidPolicy,TrustedSchemaRegistryPolicy"
        )
    }

// TODO: the issuer DID must be correctly inserted in the TIR
//    @Test
//    fun testTrustedIssuerRegistryPolicy() {
//        postAndVerify(readVerifiableCredential("VerifiableDiplomaWithIssuerTirRecord"), "TrustedIssuerRegistryPolicy")
//    }

    @Test
    fun testVerifiableId() {
        postAndVerify(readVerifiableCredential("VerifiableId"))
    }

    /*@Test
    fun testDeqarCredential() {
        postAndVerify(readVerifiableCredential("DeqarCredential"), "SignaturePolicy")
    }*/

    @Test
    fun testGaiaxCredential() {
        postAndVerify(readVerifiableCredential("GaiaxCredential"))
    }

    @Test
    fun testPermanentResidentCardCredential() {
        postAndVerify(readVerifiableCredential("PermanentResidentCard"))
    }

    @Test
    fun testGaiaxCredentialVp() {
        postAndVerify(readVerifiablePresentation("vp-GaiaxCredential-techquartier_fbdc"), "JsonSchemaPolicy")
    }

    @Test
    fun testVerifiableIdVp() {
        postAndVerify(readVerifiablePresentation("vp-VerifiableId"), "JsonSchemaPolicy")
    }

}
