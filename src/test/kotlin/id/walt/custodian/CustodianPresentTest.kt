package id.walt.custodian

import id.walt.credentials.w3c.VerifiablePresentation
import id.walt.credentials.w3c.toPresentableCredential
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.model.DidMethod
import id.walt.sdjwt.SDJwt
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.instanceOf
import org.junit.jupiter.api.assertThrows

class CustodianPresentTest : StringSpec() {
    lateinit var did: String
    lateinit var vcJsonLd: String
    lateinit var vcJwt: String

    override suspend fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)

        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")

        did = DidService.create(DidMethod.key)
        val didDoc = DidService.load(did)
        val vm = didDoc.assertionMethod!!.first().id

        println("Generated: $did")

        vcJsonLd = Signatory.getService().issue(
            "VerifiableDiploma", ProofConfig(
                issuerDid = did,
                subjectDid = did,
                issuerVerificationMethod = vm, proofType = ProofType.LD_PROOF
            )
        )

        vcJwt = Signatory.getService().issue(
            "VerifiableDiploma", ProofConfig(
                issuerDid = did,
                subjectDid = did,
                issuerVerificationMethod = vm, proofType = ProofType.JWT
            )
        )
    }

    init {
        "Json ld presentation" {
            val presStr = Custodian.getService().createPresentation(listOf(vcJsonLd.toPresentableCredential()), did, did, null, null, null)
            println("Created VP: $presStr")

            val pres = presStr.toVerifiableCredential()

            pres shouldBe instanceOf<VerifiablePresentation>()
        }

        "Jwt presentation" {
            val presStr = Custodian.getService().createPresentation(listOf(vcJwt.toPresentableCredential()), did, did, null, "abcd", null)
            println("Created VP: $presStr")

            checkVerifiablePresentation(presStr)
        }

        "Jwt presentation without audience or nonce" {
            val presStr = Custodian
                .getService()
                .createPresentation(listOf(vcJwt.toPresentableCredential()), did, null, null, "abcd", null)
            println("Created VP: $presStr")

            checkVerifiablePresentation(presStr)
        }

        "Jwt presentation without nonce" {
            val presStr = Custodian
                .getService()
                .createPresentation(listOf(vcJwt.toPresentableCredential()), did, did, null, null, null)
            println("Created VP: $presStr")

            checkVerifiablePresentation(presStr)
        }

        "Json ld and jwt presentation" {
            assertThrows<IllegalStateException> {
                Custodian
                    .getService()
                    .createPresentation(listOf(vcJsonLd, vcJwt).map { it.toPresentableCredential() }, did, did, null, "abcd", null)
            }
        }
    }

    private fun checkVerifiablePresentation(presStr: String) {
        SDJwt.isSDJwt(presStr) shouldBe true

        val pres = presStr.toVerifiableCredential()

        pres shouldBe instanceOf<VerifiablePresentation>()
        pres.sdJwt shouldNotBe null
        pres.sdJwt.toString() shouldBe presStr
    }
}
