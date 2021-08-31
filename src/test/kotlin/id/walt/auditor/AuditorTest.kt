package id.walt.auditor

import id.walt.auditor.AuditorService
import id.walt.auditor.JsonSchemaPolicy
import id.walt.auditor.SignaturePolicy
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.vc.JsonLdCredentialService
import id.walt.signatory.ProofConfig
import id.walt.signatory.Signatory
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeSameSizeAs
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

private lateinit var did: String
private lateinit var vcStr: String
private lateinit var vpStr: String

class AuditorCommandTest : StringSpec({
    ServiceMatrix("service-matrix.properties")

    val signatory = Signatory.getService()
    val credentialService = JsonLdCredentialService.getService()

    "1. generate key" {
        did = DidService.create(DidMethod.key)

        println("Generated: $did")
    }

    "2. issue vc" {
        vcStr = signatory.issue(
            "VerifiableDiploma", ProofConfig(
                issuerDid = did,
                subjectDid = did,
                issuerVerificationMethod = "Ed25519Signature2018"
            )
        )
    }

    "3. present vc" {
        vpStr = credentialService.present(vcStr, "https://api.preprod.ebsi.eu", "d04442d3-661f-411e-a80f-42f19f594c9d")
    }

    "4. verify vp" {
        val res = AuditorService.verify(vpStr, listOf(SignaturePolicy(), JsonSchemaPolicy()))

        res.overallStatus shouldBe true

        res.policyResults.keys shouldBeSameSizeAs listOf(SignaturePolicy(), JsonSchemaPolicy())

        res.policyResults.keys shouldContainAll
                listOf(SignaturePolicy(), JsonSchemaPolicy()).map { it.id }

        res.policyResults.values.forEach {
            it shouldBe true
        }
    }
})
