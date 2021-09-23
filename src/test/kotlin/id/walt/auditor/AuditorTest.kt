package id.walt.auditor

import com.github.fge.jsonschema.main.JsonSchema
import id.walt.auditor.AuditorService
import id.walt.auditor.JsonSchemaPolicy
import id.walt.auditor.SignaturePolicy
import id.walt.custodian.CustodianService
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.vc.JsonLdCredentialService
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeSameSizeAs
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

class AuditorCommandTest : StringSpec() {
    private lateinit var did: String
    private lateinit var vcStr: String
    private lateinit var vcJwt: String
    private lateinit var vpStr: String
    private lateinit var vpJwt: String

    override fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)

        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")

        val signatory = Signatory.getService()
        val custodian = CustodianService.getService()

        did = DidService.create(DidMethod.key)

        println("Generated: $did")

        vcStr = signatory.issue(
            "VerifiableDiploma", ProofConfig(
                issuerDid = did,
                subjectDid = did,
                issuerVerificationMethod = "Ed25519Signature2018", proofType = ProofType.LD_PROOF
            )
        )

        vpStr =
            custodian.createPresentation(listOf(vcStr), did, did, "https://api.preprod.ebsi.eu", "d04442d3-661f-411e-a80f-42f19f594c9d")

        vcJwt = signatory.issue(
            "VerifiableDiploma", ProofConfig(
                issuerDid = did,
                subjectDid = did,
                issuerVerificationMethod = "Ed25519Signature2018", proofType = ProofType.JWT
            )
        )

        vpJwt = custodian.createPresentation(listOf(vcJwt), did, did, null, "abcd")
    }

    init {

        "1. verify vp" {
            val res = AuditorService.verify(vpStr, listOf(SignaturePolicy(), JsonSchemaPolicy()))

            res.overallStatus shouldBe true

            res.policyResults.keys shouldBeSameSizeAs listOf(SignaturePolicy(), JsonSchemaPolicy())

            res.policyResults.keys shouldContainAll
                    listOf(SignaturePolicy(), JsonSchemaPolicy()).map { it.id }

            res.policyResults.values.forEach {
                it shouldBe true
            }
        }

        "2. verify vc" {
            val res = AuditorService.verify(vcStr, listOf(SignaturePolicy(), JsonSchemaPolicy()))

            res.overallStatus shouldBe true
            res.policyResults.keys shouldBeSameSizeAs listOf(SignaturePolicy(), JsonSchemaPolicy())

            res.policyResults.keys shouldContainAll
                    listOf(SignaturePolicy(), JsonSchemaPolicy()).map { it.id }

            res.policyResults.values.forEach {
                it shouldBe true
            }
        }

        "3. verify vc jwt" {
            val res = AuditorService.verify(vcJwt, listOf(SignaturePolicy(), JsonSchemaPolicy()))

            res.overallStatus shouldBe true
            res.policyResults.keys shouldBeSameSizeAs listOf(SignaturePolicy(), JsonSchemaPolicy())

            res.policyResults.keys shouldContainAll
                    listOf(SignaturePolicy(), JsonSchemaPolicy()).map { it.id }

            res.policyResults.values.forEach {
                it shouldBe true
            }
        }

        "4. verify vp jwt" {
            val res = AuditorService.verify(vpJwt, listOf(SignaturePolicy(), JsonSchemaPolicy()))

            res.overallStatus shouldBe true

            res.policyResults.keys shouldBeSameSizeAs listOf(SignaturePolicy(), JsonSchemaPolicy())

            res.policyResults.keys shouldContainAll
                    listOf(SignaturePolicy(), JsonSchemaPolicy()).map { it.id }

            res.policyResults.values.forEach {
                it shouldBe true
            }
        }
    }
}
