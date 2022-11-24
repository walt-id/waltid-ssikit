package id.walt.auditor

import com.beust.klaxon.JsonObject
import id.walt.auditor.dynamic.DynamicPolicy
import id.walt.auditor.dynamic.DynamicPolicyArg
import id.walt.custodian.Custodian
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.signatory.dataproviders.MergingDataProvider
import id.walt.test.RESOURCES_PATH
import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.collections.shouldBeSameSizeAs
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
@Ignored // TODO: Ignored test since ebsi dids are currently not resolving
class AuditorCommandTest : StringSpec() {
    private lateinit var did: String
    private lateinit var vcStr: String
    private lateinit var vcJwt: String
    private lateinit var vpStr: String
    private lateinit var vpJwt: String
    val enableOPATests = kotlin.runCatching { ProcessBuilder("opa").start().waitFor() == 0 }.getOrElse { false }

    override suspend fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)

        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")

        val signatory = Signatory.getService()
        val custodian = Custodian.getService()

        did = DidService.create(DidMethod.key)
        val didDoc = DidService.load(did)
        val vm = didDoc.assertionMethod!!.first().id

        println("Generated: $did")
        vcStr = signatory.issue(
            "VerifiableDiploma", ProofConfig(
                issuerDid = did,
                subjectDid = did,
                issuerVerificationMethod = vm,
                proofPurpose = "assertionMethod",
                proofType = ProofType.LD_PROOF
            )
        )

        vpStr = custodian.createPresentation(
            listOf(vcStr), did, did, "https://api.preprod.ebsi.eu", "d04442d3-661f-411e-a80f-42f19f594c9d", null
        )

        println(vpStr)

        vcJwt = signatory.issue(
            "VerifiableDiploma", ProofConfig(
                issuerDid = did, subjectDid = did, issuerVerificationMethod = vm, proofType = ProofType.JWT
            )
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
            val res =
                Auditor.getService().verify(vcStr, listOf(SignaturePolicy(), TrustedSchemaRegistryPolicy(), JsonSchemaPolicy()))

            res.valid shouldBe true
            res.policyResults.keys shouldBeSameSizeAs listOf(
                SignaturePolicy(), TrustedSchemaRegistryPolicy(), JsonSchemaPolicy()
            )

            res.policyResults.keys shouldContainAll listOf(SignaturePolicy(), TrustedSchemaRegistryPolicy()).map { it.id }

            res.policyResults.values.forEach {
                it shouldBe true
            }
        }

        "3. verify vc jwt" {
            val res =
                Auditor.getService().verify(vcJwt, listOf(SignaturePolicy(), TrustedSchemaRegistryPolicy(), JsonSchemaPolicy()))

            res.valid shouldBe true
            res.policyResults.keys shouldBeSameSizeAs listOf(
                SignaturePolicy(), TrustedSchemaRegistryPolicy(), JsonSchemaPolicy()
            )

            res.policyResults.keys shouldContainAll listOf(SignaturePolicy(), TrustedSchemaRegistryPolicy()).map { it.id }

            res.policyResults.values.forEach {
                it shouldBe true
            }
        }

        "4. verify vp jwt" {
            val res =
                Auditor.getService().verify(vpJwt, listOf(SignaturePolicy(), TrustedSchemaRegistryPolicy(), JsonSchemaPolicy()))

            res.valid shouldBe true

            res.policyResults.keys shouldBeSameSizeAs listOf(
                SignaturePolicy(), TrustedSchemaRegistryPolicy(), JsonSchemaPolicy()
            )

            res.policyResults.keys shouldContainAll listOf(SignaturePolicy(), TrustedSchemaRegistryPolicy()).map { it.id }

            res.policyResults.values.forEach {
                it shouldBe true
            }
        }

        // CLI call for testing VerifiableMandatePolicy
        // ./ssikit.sh -v vc verify rego-vc.json -p VerifiableMandatePolicy='{"user": "did:ebsi:ze2dC9GezTtVSzjHVMQzpkE", "action": "apply_to_masters", "location": "Slovenia"}'
        "5. verifiable mandate policy".config(enabled = enableOPATests) {
            val mandateSubj = mapOf(
                "credentialSubject" to mapOf(
                    "id" to did,
                    "policySchemaURI" to "https://raw.githubusercontent.com/walt-id/waltid-ssikit/master/src/test/resources/verifiable-mandates/test-policy.rego",
                    "holder" to mapOf(
                        "role" to "family",
                        "grant" to "apply_to_masters",
                        "id" to did,
                        "constraints" to mapOf("location" to "Slovenia")
                    )
                )
            )
            val mandate = Signatory.getService().issue(
                "VerifiableMandate",
                config = ProofConfig(issuerDid = did, subjectDid = did, proofType = ProofType.LD_PROOF),
                dataProvider = MergingDataProvider(mandateSubj)
            )

            /*val verificationResult = Auditor.getService().verify(mandate, mapOf(VerifiableMandatePolicy() to mapOf(
                "user" to did, "action" to "apply_to_masters", "location" to "Slovenia"
            )))*/
            println("Mandate:\n$mandate")
            val query = mapOf("user" to did, "action" to "apply_to_masters", "location" to "Slovenia")
            println("Testing query: $query")
            val verificationResult = Auditor.getService()
                .verify(mandate, listOf(PolicyRegistry.getPolicy("VerifiableMandatePolicy", JsonObject(query))))
            verificationResult.valid shouldBe true
        }

        // CLI call for testing RegoPolicy
        // ./ssikit.sh -v vc verify rego-vc.json -p RegoPolicy='{"dataPath" : "$.credentialSubject.holder", "input" : "{\"user\": \"did:ebsi:ze2dC9GezTtVSzjHVMQzpkE\", \"action\": \"apply_to_masters\", \"location\": \"Slovenia\" }", "rego" : "src/test/resources/rego/test-policy.rego", "resultPath" : "$.result[0].expressions[0].value.allow"}'
        "6. dynamic policy".config(enabled = enableOPATests) {
            // Successful testcase
            val query = mapOf("user" to did)
            println("Testing query: $query")
            val verificationResult = Auditor.getService().verify(
                vcStr,
                listOf(
                    DynamicPolicy(
                        DynamicPolicyArg(
                            input = query,
                            policy = "src/test/resources/rego/subject-policy.rego"
                        )
                    )
                )
            )
            verificationResult.valid shouldBe true

            // Successful testcase with Rego Policy Arg str
            val verificationResultStr = Auditor.getService().verify(
                vcStr,
                listOf(
                    PolicyRegistry.getPolicyWithJsonArg(
                        "DynamicPolicy",
                        "{\"dataPath\" : \"\$.credentialSubject\", \"input\" : {\"user\": \"$did\" }, \"policy\" : \"src/test/resources/rego/subject-policy.rego\"}"
                    )
                )
            ).valid
            verificationResultStr shouldBe true

            // Unsuccessful testcase
            val negQuery = mapOf("user" to "did:key:1234")
            val negResult = Auditor.getService().verify(
                vcStr,
                listOf(
                    DynamicPolicy(
                        DynamicPolicyArg(
                            input = negQuery,
                            policy = "src/test/resources/rego/subject-policy.rego"
                        )
                    )
                )
            )
            negResult.valid shouldBe false
        }
    }
}
