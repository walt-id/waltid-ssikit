package id.walt.custodian

import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.test.RESOURCES_PATH
import id.walt.vclib.credentials.VerifiablePresentation
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.model.toCredential
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.assertThrows

class CustodianPresentTest : StringSpec() {
    lateinit var did: String
    lateinit var vcJsonLd: String
    lateinit var vcJwt: String

    override fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)

        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")

        did = DidService.create(DidMethod.key)

        println("Generated: $did")

        vcJsonLd = Signatory.getService().issue(
            "VerifiableDiploma", ProofConfig(
                issuerDid = did,
                subjectDid = did,
                issuerVerificationMethod = "Ed25519Signature2018", proofType = ProofType.LD_PROOF
            )
        )

        vcJwt = Signatory.getService().issue(
            "VerifiableDiploma", ProofConfig(
                issuerDid = did,
                subjectDid = did,
                issuerVerificationMethod = "Ed25519Signature2018", proofType = ProofType.JWT
            )
        )
    }

    init {
        "Json ld presentation" {
            val presStr = Custodian.getService().createPresentation(listOf(vcJsonLd), did, did, null, null)
            println("Created VP: $presStr")

            val pres = presStr.toCredential()

            VerifiablePresentation::class.java.isAssignableFrom(pres::class.java) shouldBe true
        }

        "Jwt presentation" {
            val presStr = Custodian.getService().createPresentation(listOf(vcJwt), did, did, null, "abcd")
            println("Created VP: $presStr")

            checkVerifiablePresentation(presStr)
        }

        "Jwt presentation without audience or nonce" {
            val presStr = Custodian
                .getService()
                .createPresentation(listOf(vcJwt), did, null, null, "abcd")
            println("Created VP: $presStr")

            checkVerifiablePresentation(presStr)
        }

        "Jwt presentation without nonce" {
            val presStr = Custodian
                .getService()
                .createPresentation(listOf(vcJwt), did, did, null)
            println("Created VP: $presStr")

            checkVerifiablePresentation(presStr)
        }

        "Json ld and jwt presentation" {
            assertThrows<IllegalStateException> {
                Custodian
                    .getService()
                    .createPresentation(listOf(vcJsonLd, vcJwt), did, did, null, "abcd")
            }
        }
    }

    private fun checkVerifiablePresentation(presStr: String) {
        VerifiableCredential.isJWT(presStr) shouldBe true

        val pres = presStr.toCredential()

        VerifiablePresentation::class.java.isAssignableFrom(pres::class.java) shouldBe true
        pres.jwt shouldNotBe null
        pres.jwt shouldBe presStr
    }
}
