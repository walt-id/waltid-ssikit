package id.walt.auditor

import id.walt.custodian.Custodian
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.test.DummySignatoryDataProvider
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
        val custodian = Custodian.getService()

        did = DidService.create(DidMethod.key)

        println("Generated: $did")
        vcStr = signatory.issue("VerifiableDiploma", ProofConfig(
            issuerDid = did,
            subjectDid = did,
            issuerVerificationMethod = "Ed25519Signature2018",
            proofPurpose = "Testing",
            proofType = ProofType.LD_PROOF
        ))

        vpStr =
            custodian.createPresentation(listOf(vcStr), did, did, "https://api.preprod.ebsi.eu", "d04442d3-661f-411e-a80f-42f19f594c9d", null)

        vcJwt = signatory.issue(
            "VerifiableDiploma", ProofConfig(
                issuerDid = did,
                subjectDid = did,
                issuerVerificationMethod = "Ed25519Signature2018", proofType = ProofType.JWT
            ),
            // Required at the moment because EBSI did not upgrade V_ID schema with necessary changes.
            DummySignatoryDataProvider()
        )

        vpJwt = custodian.createPresentation(listOf(vcJwt), did, did, null, "abcd", null)
    }

    init {

        "1. verify vp" {
            val policyList = listOf(SignaturePolicy(), TrustedSchemaRegistryPolicy(), JsonSchemaPolicy())
            val res = Auditor.getService().verify(vpStr, policyList)

            res.valid shouldBe true

            res.policyResults.keys shouldBeSameSizeAs policyList

            res.policyResults.keys shouldContainAll policyList.map { it.id }

            res.policyResults.values.forEach {
                it shouldBe true
            }
        }

        "2. verify vc" {
            val res = Auditor.getService().verify(vcStr, listOf(SignaturePolicy(), TrustedSchemaRegistryPolicy(), JsonSchemaPolicy()))

            res.valid shouldBe true
            res.policyResults.keys shouldBeSameSizeAs listOf(SignaturePolicy(), TrustedSchemaRegistryPolicy(), JsonSchemaPolicy())

            res.policyResults.keys shouldContainAll
                    listOf(SignaturePolicy(), TrustedSchemaRegistryPolicy()).map { it.id }

            res.policyResults.values.forEach {
                it shouldBe true
            }
        }

        "3. verify vc jwt" {
            val res = Auditor.getService().verify(vcJwt, listOf(SignaturePolicy(), TrustedSchemaRegistryPolicy(), JsonSchemaPolicy()))

            res.valid shouldBe true
            res.policyResults.keys shouldBeSameSizeAs listOf(SignaturePolicy(), TrustedSchemaRegistryPolicy(), JsonSchemaPolicy())

            res.policyResults.keys shouldContainAll
                    listOf(SignaturePolicy(), TrustedSchemaRegistryPolicy()).map { it.id }

            res.policyResults.values.forEach {
                it shouldBe true
            }
        }

        "4. verify vp jwt" {
            val res = Auditor.getService().verify(vpJwt, listOf(SignaturePolicy(), TrustedSchemaRegistryPolicy(), JsonSchemaPolicy()))

            res.valid shouldBe true

            res.policyResults.keys shouldBeSameSizeAs listOf(SignaturePolicy(), TrustedSchemaRegistryPolicy(), JsonSchemaPolicy())

            res.policyResults.keys shouldContainAll
                    listOf(SignaturePolicy(), TrustedSchemaRegistryPolicy()).map { it.id }

            res.policyResults.values.forEach {
                it shouldBe true
            }
        }
    }
}
