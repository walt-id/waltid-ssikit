package id.walt.rest

import id.walt.auditor.Auditor
import id.walt.auditor.SignaturePolicy
import id.walt.model.DidMethod
import id.walt.rest.custodian.CustodianAPI
import id.walt.rest.custodian.PresentCredentialsRequest
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.vclib.credentials.VerifiablePresentation
import id.walt.vclib.model.toCredential
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.http.*

class CustodianApiTest : StringSpec({

    ServiceMatrix("service-matrix.properties")

    val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer(kotlinx.serialization.json.Json { encodeDefaults = false })
        }
    }

    println("${CustodianAPI.DEFAULT_BIND_ADDRESS}/${CustodianAPI.DEFAULT_Custodian_API_PORT}")

    "Starting Custodian API" {
        CustodianAPI.start()
    }

    "Check Custodian Presentation generation LD_PROOF" {
        val did = DidService.create(DidMethod.key)

        // Issuance is Signatory stuff, we're just testing the Custodian here
        val vcJwt = Signatory.getService().issue(
            "VerifiableDiploma",
            ProofConfig(
                issuerDid = did,
                subjectDid = did,
                issuerVerificationMethod = "Ed25519Signature2018",
                proofType = ProofType.LD_PROOF
            )
        )

        val response: String =
            client.post("http://${CustodianAPI.DEFAULT_BIND_ADDRESS}:${CustodianAPI.DEFAULT_Custodian_API_PORT}/credentials/present") {
                contentType(ContentType.Application.Json)
                body = PresentCredentialsRequest(listOf(vcJwt), did)
            }

        val vp = response.toCredential() as VerifiablePresentation

        vp.type shouldBe VerifiablePresentation.type

        println("VP Response: $response")

        Auditor.getService().verify(response, listOf(SignaturePolicy())).valid shouldBe true
    }

    "Check Custodian Presentation generation JWT" {
        val did = DidService.create(DidMethod.key)

        // Issuance is Signatory stuff, we're just testing the Custodian here
        val vcJwt = Signatory.getService().issue(
            "VerifiableDiploma",
            ProofConfig(
                issuerDid = did,
                subjectDid = did,
                issuerVerificationMethod = "Ed25519Signature2018",
                proofType = ProofType.JWT
            )
        )

        val response: String =
            client.post("http://${CustodianAPI.DEFAULT_BIND_ADDRESS}:${CustodianAPI.DEFAULT_Custodian_API_PORT}/credentials/present") {
                contentType(ContentType.Application.Json)
                body = PresentCredentialsRequest(listOf(vcJwt), did)
            }

        response.count { it == '.' } shouldBe 2

        println("VP Response: $response")

        Auditor.getService().verify(response, listOf(SignaturePolicy())).valid shouldBe true
    }

    /*"Test documentation" {
        val response = get("/v1/api-documentation").readText()

        response shouldContain "\"operationId\":\"health\""
        response shouldContain "Returns HTTP 200 in case all services are up and running"
    }*/
})








