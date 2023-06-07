package id.walt.services

import id.walt.auditor.Auditor
import id.walt.auditor.VerificationPolicyResult
import id.walt.auditor.policies.JsonSchemaPolicy
import id.walt.auditor.policies.SignaturePolicy
import id.walt.credentials.w3c.schema.SchemaValidator
import id.walt.credentials.w3c.schema.SchemaValidatorFactory
import id.walt.credentials.w3c.toPresentableCredential
import id.walt.custodian.Custodian
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.StringSpec
import io.mockk.every
import io.mockk.mockkObject
import java.net.URI

class ReadmeTest : StringSpec({
    "Check README.md example code" {
        fun main() {
            // Load services
            ServiceMatrix("service-matrix.properties")

            // Create DIDs
            val issuerDid = DidService.create(DidMethod.ebsi)
            val holderDid = DidService.create(DidMethod.key)

            // Issue VC with LD_PROOF and JWT format (for show-casing both formats)
            val vcJson = Signatory.getService().issue(
                templateIdOrFilename = "VerifiableId",
                config = ProofConfig(issuerDid = issuerDid, subjectDid = holderDid, proofType = ProofType.LD_PROOF)
            )
            val vcJwt = Signatory.getService().issue(
                templateIdOrFilename = "Europass",
                config = ProofConfig(issuerDid = issuerDid, subjectDid = holderDid, proofType = ProofType.JWT)
            )

            // Present VC in JSON-LD and JWT format (for show-casing both formats)
            val vpJson = Custodian.getService().createPresentation(listOf(vcJson.toPresentableCredential()), holderDid)
            val vpJwt = Custodian.getService().createPresentation(listOf(vcJwt.toPresentableCredential()), holderDid)

            // Verify VPs, using Signature, JsonSchema and a custom policy
            val resJson = Auditor.getService().verify(vpJson, listOf(SignaturePolicy(), JsonSchemaPolicy()))
            val resJwt = Auditor.getService().verify(vpJwt, listOf(SignaturePolicy(), JsonSchemaPolicy()))

            println("JSON verification result: ${resJson.policyResults}")
            println("JWT verification result:  ${resJwt.policyResults}")
        }
        main()
    }
}) {
    override suspend fun beforeSpec(spec: Spec) {
        mockkObject(SchemaValidatorFactory)
        every { SchemaValidatorFactory.get(any<URI>()) }.returns(object : SchemaValidator {
            override fun validate(json: String): VerificationPolicyResult {
                return VerificationPolicyResult.success()
            }

        })
    }
}
