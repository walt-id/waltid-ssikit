package id.walt.custodian

import id.walt.auditor.AuditorService
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.vclib.Helpers.toCredential
import id.walt.vclib.VcLibManager
import id.walt.vclib.vclist.VerifiablePresentation
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import okio.ByteString.Companion.encode

class CustodianPresentTest : StringSpec() {
    lateinit var did: String
    lateinit var vcJsonLd: String
    lateinit var vcJwt: String

    override fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)

        ServiceMatrix("service-matrix.properties")

        did = DidService.create(DidMethod.key)

        println("Generated: $did")

        vcJsonLd = Signatory.getService().issue(
            "VerifiableDiploma", ProofConfig(
                issuerDid = did,
                subjectDid = did,
                issuerVerificationMethod = "Ed25519Signature2018", ProofType.LD_PROOF)
        )

        vcJwt = Signatory.getService().issue(
            "VerifiableDiploma", ProofConfig(
                issuerDid = did,
                subjectDid = did,
                issuerVerificationMethod = "Ed25519Signature2018", ProofType.JWT)
        )
    }

    init {
        "1: json ld presentation" {
            val presStr = CustodianService.getService().createPresentation(vcJsonLd, null, null)
            println("Created VP: ${presStr}")

            val pres = presStr.toCredential()

            VerifiablePresentation::class.java.isAssignableFrom(pres::class.java) shouldBe true
        }

        "2: jwt presentation" {
            val presStr = CustodianService.getService().createPresentation(vcJwt, null, null)
            println("Created VP: ${presStr}")

            VcLibManager.isJWT(presStr) shouldBe true

            val pres = presStr.toCredential()

            VerifiablePresentation::class.java.isAssignableFrom(pres::class.java) shouldBe true
            pres.jwt shouldNotBe null
            pres.jwt shouldBe presStr
        }
    }
}