package id.walt.signatory

import com.beust.klaxon.Klaxon
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.signatory.rest.IssueCredentialRequest
import id.walt.signatory.rest.SignatoryRestAPI
import id.walt.test.RESOURCES_PATH
import id.walt.vclib.credentials.*
import id.walt.vclib.model.toCredential
import id.walt.vclib.templates.VcTemplateManager
import io.github.rybalkinsd.kohttp.dsl.httpPost
import io.github.rybalkinsd.kohttp.ext.asString
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking

class SignatoryApiTest : AnnotationSpec() {

    init {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    }

    val SIGNATORY_API_HOST = "localhost"
    val SIGNATORY_API_PORT = 7001
    val SIGNATORY_API_URL = "http://$SIGNATORY_API_HOST:$SIGNATORY_API_PORT"

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
        expectSuccess = false
    }

    @BeforeClass
    fun startServer() {
        SignatoryRestAPI.start(SIGNATORY_API_PORT)
    }

    @AfterClass
    fun teardown() {
        SignatoryRestAPI.stop()
    }

    @Test
    fun testHealth() = runBlocking {
        val response = client.get("$SIGNATORY_API_URL/health").bodyAsText()
        response shouldBe "OK"
    }

    @Test
    fun testListVcTemplates() = runBlocking {
        val templates = client.get("$SIGNATORY_API_URL/v1/templates") {
            contentType(ContentType.Application.Json)
        }.body<List<String>>()

        VcTemplateManager.getTemplateList().forEach { templateName -> templates shouldContain templateName }

        templates shouldContain "Europass"
        templates shouldContain "VerifiablePresentation"
    }


    @Test
    fun testLoadVcTemplates() = runBlocking {

        VcTemplateManager.getTemplateList().forEach { templateName ->

            val templateJson = client.get("$SIGNATORY_API_URL/v1/templates/$templateName") {
                contentType(ContentType.Application.Json)
            }.bodyAsText()

            println(templateJson)
            println("should contain $templateName")

            (templateJson.contains(templateName) || templateJson.contains("Experimental")) shouldBe true
        }
    }

    @Test
    fun testLoadEuropass() = runBlocking {
        val europassJson = client.get("$SIGNATORY_API_URL/v1/templates/Europass") {
            contentType(ContentType.Application.Json)
        }.bodyAsText()

        europassJson shouldEqualJson Europass.template!!.invoke().encode()
    }

    @Test
    fun testIssueVerifiableDiplomaJsonLd() = runBlocking {
        val did = DidService.create(DidMethod.key)
        val didDoc = DidService.load(did)
        val vm = didDoc.assertionMethod!!.first().id

        val vc = httpPost {
            host = SIGNATORY_API_HOST
            port = SIGNATORY_API_PORT
            path = "/v1/credentials/issue"

            body {
                json(
                    Klaxon().toJsonString(
                        IssueCredentialRequest(
                            "VerifiableDiploma",
                            ProofConfig(
                                issuerDid = did,
                                subjectDid = did,
                                issuerVerificationMethod = vm,
                                proofType = ProofType.LD_PROOF
                            )
                        )
                    )
                )
            }
        }.asString()

        println(vc)
        val cred = vc?.toCredential() as VerifiableDiploma
        cred.issuer shouldBe did
        cred.credentialSubject?.id shouldBe did
        cred.proof?.verificationMethod shouldBe vm
    }

    @Test
    fun testIssueVerifiableIdJwt() = runBlocking {
        val did = DidService.create(DidMethod.ebsi)
        val didDoc = DidService.load(did)
        val vm = didDoc.assertionMethod!!.first().id

        val vc = httpPost {
            host = SIGNATORY_API_HOST
            port = SIGNATORY_API_PORT
            path = "/v1/credentials/issue"

            body {
                json(
                    Klaxon().toJsonString(
                        IssueCredentialRequest(
                            "VerifiableId",
                            ProofConfig(
                                issuerDid = did,
                                subjectDid = did,
                                issuerVerificationMethod = vm,
                                proofType = ProofType.JWT
                            )
                        )
                    )
                )
            }
        }.asString()

        println(vc)
        vc shouldStartWith "ey"
    }

    @Test
    fun testIssueEuropassJsonLd() = runBlocking {
        val did = DidService.create(DidMethod.ebsi)
        val didDoc = DidService.load(did)
        val vm = didDoc.assertionMethod!!.first().id

        val vc = httpPost {
            host = SIGNATORY_API_HOST
            port = SIGNATORY_API_PORT
            path = "/v1/credentials/issue"

            body {
                json(
                    Klaxon().toJsonString(
                        IssueCredentialRequest(
                            "Europass",
                            ProofConfig(
                                issuerDid = did,
                                subjectDid = did,
                                issuerVerificationMethod = vm,
                                proofType = ProofType.LD_PROOF
                            )
                        )
                    )
                )
            }
        }.also { it.code shouldBe 200 }.asString()

        println(vc)
        val cred = vc?.toCredential() as Europass
        cred.issuer shouldBe did
        cred.credentialSubject?.id shouldBe did
        cred.proof?.verificationMethod shouldBe vm
    }

    @Test
    fun testIssuePermanentResidentCardJsonLd() = runBlocking {
        val did = DidService.create(DidMethod.ebsi)
        val didDoc = DidService.load(did)
        val vm = didDoc.assertionMethod!!.first().id

        val vc = httpPost {
            host = SIGNATORY_API_HOST
            port = SIGNATORY_API_PORT
            path = "/v1/credentials/issue"

            body {
                json(
                    Klaxon().toJsonString(
                        IssueCredentialRequest(
                            "PermanentResidentCard",
                            ProofConfig(
                                issuerDid = did,
                                subjectDid = did,
                                issuerVerificationMethod = vm,
                                proofType = ProofType.LD_PROOF
                            )
                        )
                    )
                )
            }
        }.asString()

        println(vc)
        val cred = vc?.toCredential() as PermanentResidentCard
        cred.issuer shouldBe did
        cred.credentialSubject?.id shouldBe did
        cred.proof?.verificationMethod shouldBe vm
    }

    @Test
    fun testIssueVerifiableAuthorizationJsonLd() = runBlocking {
        val did = DidService.create(DidMethod.ebsi)
        val didDoc = DidService.load(did)
        val vm = didDoc.assertionMethod!!.first().id

        val vc = httpPost {
            host = SIGNATORY_API_HOST
            port = SIGNATORY_API_PORT
            path = "/v1/credentials/issue"

            body {
                json(
                    Klaxon().toJsonString(
                        IssueCredentialRequest(
                            "VerifiableAuthorization",
                            ProofConfig(
                                issuerDid = did,
                                subjectDid = did,
                                issuerVerificationMethod = vm,
                                proofType = ProofType.LD_PROOF
                            )
                        )
                    )
                )
            }
        }.asString()

        println(vc)
        val cred = vc?.toCredential() as VerifiableAuthorization
        cred.issuer shouldBe did
        cred.credentialSubject?.id shouldBe did
        cred.proof?.verificationMethod shouldBe vm
    }

    @Test
    fun testIssueVerifiableAttestationJsonLd() = runBlocking {
        val did = DidService.create(DidMethod.key)
        val didDoc = DidService.load(did)
        val vm = didDoc.assertionMethod!!.first().id

        val vc = httpPost {
            host = SIGNATORY_API_HOST
            port = SIGNATORY_API_PORT
            path = "/v1/credentials/issue"

            body {
                json(
                    Klaxon().toJsonString(
                        IssueCredentialRequest(
                            "VerifiableAttestation",
                            ProofConfig(
                                issuerDid = did,
                                subjectDid = did,
                                issuerVerificationMethod = vm,
                                proofType = ProofType.LD_PROOF
                            )
                        )
                    )
                )
            }
        }.asString()

        println(vc)
        val cred = vc?.toCredential() as VerifiableAttestation
        // cred.issuer shouldBe did // "NEW ISSUER" set by Command test
        // cred.credentialSubject?.id shouldBe did // "id123" set by Command test
        cred.proof?.verificationMethod shouldBe vm
    }
}
