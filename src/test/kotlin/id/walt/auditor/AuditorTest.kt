package id.walt.auditor

import com.beust.klaxon.JsonObject
import id.walt.auditor.dynamic.DynamicPolicy
import id.walt.auditor.dynamic.DynamicPolicyArg
import id.walt.auditor.policies.*
import id.walt.common.KlaxonWithConverters
import id.walt.common.resolveContent
import id.walt.credentials.w3c.JsonConverter
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.toPresentableCredential
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.custodian.Custodian
import id.walt.model.*
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.ecosystems.essif.TrustedIssuerClient
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.signatory.dataproviders.MergingDataProvider
import id.walt.test.RESOURCES_PATH
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldBeSameSizeAs
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.net.URI
import java.net.URL


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
            listOf(vcStr.toPresentableCredential()), did, did, "https://api-pilot.ebsi.eu", "d04442d3-661f-411e-a80f-42f19f594c9d", null
        )

        println(vpStr)

        vcJwt = signatory.issue(
            "VerifiableDiploma", ProofConfig(
                issuerDid = did, subjectDid = did, issuerVerificationMethod = vm, proofType = ProofType.JWT
            )
        )

        vpJwt = custodian.createPresentation(listOf(vcJwt.toPresentableCredential()), did, did, null, "abcd", null)
    }

    init {

        "1. verify vp" {
            val policyList = listOf(SignaturePolicy(), JsonSchemaPolicy())
            val res = Auditor.getService().verify(vpStr, policyList)

            res.result shouldBe true

            res.policyResults.keys shouldBeSameSizeAs policyList

            res.policyResults.keys shouldContainAll policyList.map { it.id }

            res.policyResults.values.forEach { it.isSuccess shouldBe true }
        }

        "2. verify vc" {
            val res =
                Auditor.getService().verify(vcStr, listOf(SignaturePolicy(), JsonSchemaPolicy()))

            res.result shouldBe true
            res.policyResults.keys shouldBeSameSizeAs listOf(
                SignaturePolicy(), JsonSchemaPolicy()
            )

            res.policyResults.keys shouldContainAll listOf(SignaturePolicy()).map { it.id }

            res.policyResults.values.forEach { it.isSuccess shouldBe true }
        }

        "3. verify vc jwt" {
            val res =
                Auditor.getService().verify(vcJwt, listOf(SignaturePolicy(), JsonSchemaPolicy()))

            res.result shouldBe true
            res.policyResults.keys shouldBeSameSizeAs listOf(
                SignaturePolicy(),  JsonSchemaPolicy()
            )

            res.policyResults.keys shouldContainAll listOf(SignaturePolicy()).map { it.id }

            res.policyResults.values.forEach { it.isSuccess shouldBe true }
        }

        "4. verify vp jwt" {
            val res =
                Auditor.getService().verify(vpJwt, listOf(SignaturePolicy(), JsonSchemaPolicy()))

            res.result shouldBe true

            res.policyResults.keys shouldBeSameSizeAs listOf(
                SignaturePolicy(),  JsonSchemaPolicy()
            )

            res.policyResults.keys shouldContainAll listOf(SignaturePolicy()).map { it.id }

            res.policyResults.values.forEach { it.isSuccess shouldBe true }
        }

        // CLI call for testing VerifiableMandatePolicy
        // ./ssikit.sh -v vc verify rego-vc.json -p VerifiableMandatePolicy='{"user": "did:ebsi:ze2dC9GezTtVSzjHVMQzpkE", "action": "apply_to_masters", "location": "Slovenia"}'
        "5. verifiable mandate policy".config(enabled = enableOPATests) {
            val mandateSubj = mapOf(
                "credentialSubject" to mapOf(
                    "id" to did,
                    "policySchemaURI" to "src/test/resources/verifiable-mandates/test-policy.rego",
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
            verificationResult.result shouldBe true
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
            verificationResult.result shouldBe true

            // Successful testcase with Rego Policy Arg str
            val verificationResultStr = Auditor.getService().verify(
                vcStr,
                listOf(
                    PolicyRegistry.getPolicyWithJsonArg(
                        "DynamicPolicy",
                        "{\"dataPath\" : \"\$\", \"input\" : {\"user\": \"$did\" }, \"policy\" : \"src/test/resources/rego/subject-policy.rego\"}"
                    )
                )
            ).result
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
            negResult.result shouldBe false
        }

        "6a. test policy example in issue #264".config(enabled = enableOPATests)  {
            val credential = File("$RESOURCES_PATH/rego/issue264/StudentCard.json").readText().toVerifiableCredential()
            val input = Json.parseToJsonElement(File("$RESOURCES_PATH/rego/issue264/input.json").readText()).jsonObject
            val dynPolArg = DynamicPolicyArg(
                input = JsonConverter.fromJsonElement(input) as Map<String, Any?>,
                policy = "$RESOURCES_PATH/rego/issue264/policy.rego"
            )
            val polResult = Auditor.getService().verify(
                credential, listOf(DynamicPolicy(dynPolArg))
            )
            polResult.result shouldBe true
        }

        "7. test JsonSchemaPolicy" {
            // test schema from credentialSchema.id property of credential
            Auditor.getService().verify(vcStr, listOf(JsonSchemaPolicy())).result shouldBe true

            // test VerifiableDiploma schema from vclib
            Auditor.getService().verify(
                vcStr,
                listOf(JsonSchemaPolicy(JsonSchemaPolicyArg("https://raw.githubusercontent.com/walt-id/waltid-ssikit-vclib/master/src/test/resources/schemas/VerifiableDiploma.json")))
            )
                .result shouldBe true

            // test VerifiableId schema from vclib (should not validate)
            Auditor.getService().verify(
                vcStr,
                listOf(JsonSchemaPolicy(JsonSchemaPolicyArg("https://raw.githubusercontent.com/walt-id/waltid-ssikit-vclib/master/src/test/resources/schemas/VerifiableId.json")))
            )
                .result shouldBe false

            // this is a VerifiableDiploma (EUROPASS) schema, which our VerifiableDiploma template does NOT comply with:
            // https://ec.europa.eu/digital-building-blocks/wikis/display/EBSIDOC/Data+Models+and+Schemas
            val verifiableDiplomaUrl =
                URL("https://api-pilot.ebsi.eu/trusted-schemas-registry/v1/schemas/0x4dd3926cd92bb3cb64fa6c837539ed31fc30dd38a11266a91678efa7268cde09")
            runCatching { verifiableDiplomaUrl.openStream() }.onSuccess {
                Auditor.getService().verify(
                    vcStr,
                    listOf(JsonSchemaPolicy(JsonSchemaPolicyArg(verifiableDiplomaUrl.toExternalForm())))
                ).result shouldBe false
            }

            // test passing schema by value
            val schemaContent =
                URI.create("https://raw.githubusercontent.com/walt-id/waltid-ssikit-vclib/master/src/test/resources/schemas/VerifiableDiploma.json")
                    .toURL().readText()
            Auditor.getService().verify(vcStr, listOf(JsonSchemaPolicy(JsonSchemaPolicyArg(schemaContent)))).result shouldBe true

            // test passing schema by file path
            val tempFile = File.createTempFile("schema", ".json")
            tempFile.writeText(schemaContent)
            shouldNotThrowAny {
                Auditor.getService()
                    .verify(vcStr, listOf(JsonSchemaPolicy(JsonSchemaPolicyArg(tempFile.absolutePath)))).result shouldBe true
            }
            tempFile.delete()
        }

        fun validateSchema(credentialFile: String, schemaFile: String? = null) {
            val credential =
                VerifiableCredential.fromString(File("$credentialFile").readText())

            if (schemaFile.isNullOrBlank()) {
                Auditor.getService().verify(credential, listOf(JsonSchemaPolicy())).result shouldBe true
            } else {
                Auditor.getService()
                    .verify(credential, listOf(JsonSchemaPolicy(JsonSchemaPolicyArg(schemaFile)))).result shouldBe true
            }
        }

        "8. verify EBSI credentials" {

            // EBSI data models and schemas  https://ec.europa.eu/digital-building-blocks/wikis/display/EBSIDOC/Data+Models+and+Schemas
            val EBSI_VC_PATH = "src/main/resources/vc-templates/"
            val EBSI_SCHEMA_PATH = "src/test/resources/ebsi/schema/"
            val EBSI_TSR_BASE = "https://api-pilot.ebsi.eu/trusted-schemas-registry/v1/schemas"

            // VerifiableAttestation - verification from local file
            validateSchema(
                "$EBSI_VC_PATH/EbsiVerifiableAttestationGeneric.json",
                "$EBSI_SCHEMA_PATH/EbsiVerifiableAttestationSchema.json"
            )
            validateSchema(
                "$EBSI_VC_PATH/EbsiVerifiableAttestationPerson.json",
                "$EBSI_SCHEMA_PATH/EbsiVerifiableAttestationSchema.json"
            )
            validateSchema(
                "$EBSI_VC_PATH/EbsiVerifiableAttestationLegal.json",
                "$EBSI_SCHEMA_PATH/EbsiVerifiableAttestationSchema.json"
            )

            // AccreditedVerifiableAttestation
            validateSchema(
                "$EBSI_VC_PATH/EbsiAccreditedVerifiableAttestation.json",
                "$EBSI_SCHEMA_PATH/AccreditedVerifiableAttestation.json"
            )
        }

        "9. test EbsiTrustedSchemaRegistryPolicy" {
            forAll(
                row("verifiable-accreditation-pass.json", true, emptyList()),
                row("verifiable-accreditation-fail-noschema.json", false, listOf(IllegalArgumentException("Credential has no associated credentialSchema property"))),
                row("verifiable-accreditation-fail-notvalid.json", false, listOf(Throwable("No valid EBSI Trusted Schema Registry URL"))),
                row("verifiable-accreditation-fail-notavailable.json", false, listOf(Throwable("Schema not available in the EBSI Trusted Schema Registry"))),
            ) { filepath, isSuccess, message ->
                val schemaPath = "src/test/resources/ebsi/trusted-schema-registry-tests/"
                val policy = EbsiTrustedSchemaRegistryPolicy()
                val vc = resolveContent(schemaPath + filepath).toVerifiableCredential()
                val result = policy.verify(vc)

                result.isSuccess shouldBe isSuccess
                result.errors shouldBe message
            }
        }

        "10. test EbsiTrustedIssuerAccreditationPolicy" {
            forAll(
                row("TIVerifiableAccreditationTIDiploma.json", "tao-tir-attribute.json", true, emptyList<Throwable>()),
                row("TAOVerifiableAccreditation.json", "tao-tir-attribute.json", true, emptyList<Throwable>()),
            ) { vcPath, attrPath, isSuccess, errors ->
                val schemaPath = "src/test/resources/ebsi/trusted-issuer-chain/"
                val policy = EbsiTrustedIssuerAccreditationPolicy()
                val vc = resolveContent(schemaPath + vcPath).toVerifiableCredential()
                val tirRecord = resolveContent(schemaPath + attrPath)
                mockkStatic(::resolveContent)
                every { resolveContent(any()) } returns tirRecord

                val result = policy.verify(vc)

                result.isSuccess shouldBe isSuccess
                result.errors shouldBe errors

                unmockkStatic(::resolveContent)
            }
        }

        "11. test EbsiTrustedIssuerRegistryPolicy"{
            forAll(
                // self issued (tao accreditation)
                row("TAOVerifiableAccreditation.json", "tao-tir-record.json", "tao-tir-attribute.json", TrustedIssuerType.TAO, true, emptyList<Throwable>()),
                // issued by tao (ti accreditation)
                row("TIVerifiableAccreditationTIDiploma.json", "tao-tir-record.json", "tao-tir-attribute.json", TrustedIssuerType.TAO, true, emptyList<Throwable>()),
                // issued by issuer (diploma credential)
                row("VerifiableDiploma.json", "ti-tir-record.json", "tao-tir-attribute.json", TrustedIssuerType.TI, true, emptyList<Throwable>()),
            ) { vcPath, tirRecordPath, tirAttributePath, issuerType, isSuccess, errors ->
                val schemaPath = "src/test/resources/ebsi/trusted-issuer-chain/"
                val policy = EbsiTrustedIssuerRegistryPolicy(issuerType)
                val vc = resolveContent(schemaPath + vcPath).toVerifiableCredential()
                val attribute = KlaxonWithConverters().parse<AttributeRecord>(resolveContent (schemaPath + tirAttributePath))!!
                val tirRecord = KlaxonWithConverters().parse<TrustedIssuer>(resolveContent(schemaPath + tirRecordPath))!!
                mockkObject(DidService)
                mockkObject(TrustedIssuerClient)
                every { DidService.loadOrResolveAnyDid(any()) } returns Did(id = vc.issuerId!!)
                every { TrustedIssuerClient.getAttribute(any()) } returns attribute
                every { TrustedIssuerClient.getIssuer(any<TrustedIssuerType>()) } returns tirRecord

                val result = policy.verify(vc)

                result.isSuccess shouldBe isSuccess
                result.errors shouldBe errors

                unmockkObject(DidService)
                unmockkObject(TrustedIssuerClient)
            }
        }

        "12. test serialize verification result" {
            val verificationResult = VerificationResult(true, mapOf(
                "SignaturePolicy" to VerificationPolicyResult.success()
            ))

            val serializedResult = KlaxonWithConverters().toJsonString(verificationResult)

            val deserializedResult = KlaxonWithConverters().parse<VerificationResult>(serializedResult)

            verificationResult.result shouldBe deserializedResult!!.result
            verificationResult.policyResults.forEach {
                deserializedResult.policyResults[it.key]?.isSuccess shouldBe it.value.isSuccess
            }
        }
    }
}
