package id.walt.signatory

import com.beust.klaxon.Klaxon
import id.walt.common.KlaxonWithConverters
import id.walt.credentials.w3c.templates.VcTemplate
import id.walt.credentials.w3c.templates.VcTemplateService
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.model.DidMethod
import id.walt.sdjwt.SDJwt
import id.walt.sdjwt.SDMapBuilder
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.WaltIdServices
import id.walt.services.did.DidService
import id.walt.signatory.rest.IssueCredentialRequest
import id.walt.signatory.rest.SignatoryRestAPI
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URL

class SignatoryApiTest : AnnotationSpec() {

    init {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    }

    val SIGNATORY_API_HOST = "localhost"
    val SIGNATORY_API_PORT = 7001
    val SIGNATORY_API_URL = "http://$SIGNATORY_API_HOST:$SIGNATORY_API_PORT"

    val client = HttpClient(OkHttp) {
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

    private val templateService get () = VcTemplateService.getService()

    @Test
    fun testHealth() = runBlocking {
        val response = client.get("$SIGNATORY_API_URL/health").bodyAsText()
        response shouldBe "OK"
    }

    @Test
    fun testListVcTemplates() = runBlocking {
        val templates =
            client.get("$SIGNATORY_API_URL/v1/templates").bodyAsText()
                .also { println("BODY: $it") }
                .let { KlaxonWithConverters().parseArray<VcTemplate>(it) }!!
        // make sure templates are not populated
        templates.forEach {
            it.template shouldBe null
        }
        val templateNames = templates.map { it.name }

        templateNames shouldContain "Europass"
        templateNames shouldContain "VerifiablePresentation"

        templateService.listTemplates().map { it.name }.forEach { templateName -> templateNames shouldContain templateName }

        println(templates)
    }


    @Test
    fun testLoadVcTemplates() = runBlocking {

        templateService.listTemplates().forEach { template ->

            val templateJson = client.get("$SIGNATORY_API_URL/v1/templates/${template.name}") {
                contentType(ContentType.Application.Json)
            }.bodyAsText()

            templateJson shouldNotBe null
        }
    }

    @Test
    fun testLoadEuropass() = runBlocking {
        val europassJson = client.get("$SIGNATORY_API_URL/v1/templates/Europass") {
            contentType(ContentType.Application.Json)
        }.bodyAsText()

        europassJson.toVerifiableCredential().type shouldContain "Europass"
    }


    @Test
    fun testLoadPacketDeliveryService() = runBlocking {
        val pdsJson = client.get("$SIGNATORY_API_URL/v1/templates/PacketDeliveryService") {
            contentType(ContentType.Application.Json)
        }.bodyAsText()

        pdsJson.toVerifiableCredential().type shouldContain "PacketDeliveryService"
    }

    private fun httpPost(path: String, body: String, host: String = SIGNATORY_API_HOST, port: Int = SIGNATORY_API_PORT) =
        runBlocking {
            try {
                WaltIdServices.httpNoAuth.post(URL("http", host, port, path)) {
                    setBody(body)
                }.bodyAsText()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    @Test
    fun testIssueVerifiableDiplomaJsonLd() = runBlocking {
        val did = DidService.create(DidMethod.key)
        val didDoc = DidService.load(did)
        val vm = didDoc.assertionMethod!!.first().id

        val vc = httpPost(
            "/v1/credentials/issue", Klaxon().toJsonString(
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

        vc shouldNotBe null
        println(vc!!)
        val cred = vc.toVerifiableCredential()
        cred.issuerId shouldBe did
        cred.credentialSubject?.id shouldBe did
        cred.proof?.verificationMethod shouldBe vm
    }

    @Test
    fun testIssueVerifiableIdJwt() = runBlocking {
        val did = DidService.create(DidMethod.ebsi)
        val didDoc = DidService.load(did)
        val vm = didDoc.assertionMethod!!.first().id

        val vc = httpPost(
            "/v1/credentials/issue", Klaxon().toJsonString(
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

        println(vc)
        vc shouldStartWith "ey"
    }

    @Test
    fun testIssueEuropassJsonLd() = runBlocking {
        val did = DidService.create(DidMethod.ebsi)
        val didDoc = DidService.load(did)
        val vm = didDoc.assertionMethod!!.first().id

        val vc = httpPost(
            "/v1/credentials/issue", Klaxon().toJsonString(
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

        vc shouldNotBe null
        println(vc!!)
        val cred = vc.toVerifiableCredential()
        cred.issuerId shouldBe did
        cred.credentialSubject?.id shouldBe did
        cred.proof?.verificationMethod shouldBe vm
    }

    @Test
    fun testIssuePermanentResidentCardJsonLd() = runBlocking {
        val did = DidService.create(DidMethod.ebsi)
        val didDoc = DidService.load(did)
        val vm = didDoc.assertionMethod!!.first().id

        val vc = httpPost(
            "/v1/credentials/issue", Klaxon().toJsonString(
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

        vc shouldNotBe null
        println(vc!!)
        val cred = vc.toVerifiableCredential()
        cred.issuerId shouldBe did
        cred.credentialSubject?.id shouldBe did
        cred.proof?.verificationMethod shouldBe vm
    }

    @Test
    fun testIssueVerifiableAuthorizationJsonLd() = runBlocking {
        val did = DidService.create(DidMethod.ebsi)
        val didDoc = DidService.load(did)
        val vm = didDoc.assertionMethod!!.first().id

        val vc = httpPost(
            "/v1/credentials/issue", Klaxon().toJsonString(
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

        vc shouldNotBe null
        println(vc!!)
        val cred = vc.toVerifiableCredential()
        cred.issuerId shouldBe did
        cred.credentialSubject?.id shouldBe did
        cred.proof?.verificationMethod shouldBe vm
    }

    @Test
    fun testIssueVerifiableAttestationJsonLd() = runBlocking {
        val did = DidService.create(DidMethod.key)
        val didDoc = DidService.load(did)
        val vm = didDoc.assertionMethod!!.first().id

        val vc = httpPost(
            "/v1/credentials/issue", Klaxon().toJsonString(
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

        vc shouldNotBe null
        println(vc!!)
        val cred = vc.toVerifiableCredential()
        // cred.issuer shouldBe did // "NEW ISSUER" set by Command test
        // cred.credentialSubject?.id shouldBe did // "id123" set by Command test
        cred.proof?.verificationMethod shouldBe vm
    }

    @Test
    fun testIssueFromJson() {
        val did = DidService.create(DidMethod.key)
        val json = """
            {
                "type": [ "VerifiableCredential", "CustomCred" ],
                "credentialSubject": {
                    "foo": "bar"
                }
            }
        """.trimIndent()

        val vc = httpPost(
            "/v1/credentials/issueFromJson?issuerId=$did&subjectId=$did", json
        )

        vc shouldNotBe null
        val vcParsed = vc!!.toVerifiableCredential()
        vcParsed.type shouldBe listOf("VerifiableCredential", "CustomCred")
        vcParsed.issuerId shouldBe did
        vcParsed.subjectId shouldBe did
        vcParsed.credentialSubject!!.properties["foo"] shouldBe "bar"
    }

    @Test
    fun testIssueRequestSDMap() {
        val did = DidService.create(DidMethod.key)
        val reqObj = IssueCredentialRequest(
            templateId = "VerifiableId",
            config = ProofConfig(
                issuerDid = did,
                subjectDid = did,
                proofType = ProofType.SD_JWT,
                selectiveDisclosure = SDMapBuilder()
                    .addField("credentialSubject", true,
                        SDMapBuilder().addField("firstName", true).build()
                    ).build()
            ),
            credentialData = buildJsonObject {
                put("credentialSubject", buildJsonObject {
                    put("firstName", "Severin")
                })
            }
        )
        val vc = httpPost("/v1/credentials/issue", KlaxonWithConverters().toJsonString(reqObj))
        println(vc)
        val parsedVc = vc?.toVerifiableCredential()
        parsedVc shouldNotBe null
        parsedVc!!.credentialSubject!!.properties shouldContain Pair("firstName", "Severin")

        val parsedSdJwt = SDJwt.parse(vc)
        parsedSdJwt.undisclosedPayload shouldNotContainKey "credentialSubject"
        parsedSdJwt.disclosureObjects.map { it.key }.toSet() shouldContainAll setOf("credentialSubject", "firstName")
    }
}
