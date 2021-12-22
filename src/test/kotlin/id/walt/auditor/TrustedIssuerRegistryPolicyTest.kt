package id.walt.auditor

import id.walt.crypto.encBase64Str
import id.walt.model.Attribute
import id.walt.model.DidMethod
import id.walt.model.TrustedIssuer
import id.walt.services.did.DidService
import id.walt.services.essif.TrustedIssuerClient
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.vclib.model.toCredential
import id.walt.vclib.model.VerifiableCredential
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject

class TrustedIssuerRegistryPolicyTest : AnnotationSpec() {

    private val testedPolicy = TrustedIssuerRegistryPolicy()
    private val mockedHash = "mockHash"
    private val validAttrInfoJson = "{\"@context\":\"https://ebsi.eu\",\"type\":\"attribute\",\"name\":\"issuer\",\"data\":\"5d50b3fa18dde32b384d8c6d096869de\"}"
    private lateinit var did: String
    private lateinit var verifiableCredential: VerifiableCredential

    override fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)

        val signatory = Signatory.getService()

        did = DidService.create(DidMethod.key)

        println("Generated: $did")

        val vcStr = signatory.issue(
            "VerifiableDiploma", ProofConfig(
                issuerDid = did,
                subjectDid = did,
                issuerVerificationMethod = "Ed25519Signature2018",
                proofPurpose = "Testing",
                proofType = ProofType.LD_PROOF
            )
        )

        verifiableCredential = vcStr.toCredential()
    }

    @Test
    fun whenTrustedIssuerRegistryContainsValidAttributeThenReturnTrue() {
        val attributeList = listOf(Attribute(mockedHash, encBase64Str(validAttrInfoJson)))
        mockTrustedIssuerWithAttributes(attributeList)

        testedPolicy.verify(verifiableCredential) shouldBe true
    }

    @Test
    fun whenTrustedIssuerRegistryContainsInvalidBase64AttributeThenReturnFalse() {
        val attributeList = listOf(Attribute(mockedHash, "invalidBase64EncodedString"))
        mockTrustedIssuerWithAttributes(attributeList)

        testedPolicy.verify(verifiableCredential) shouldBe false
    }

    @Test
    fun whenTrustedIssuerRegistryContainsMultipleAttributesWithLastValidThenReturnTrue() {
        val attr1 = Attribute(mockedHash, "invalidBase64EncodedString")
        val attr2 = Attribute(mockedHash, encBase64Str("invalidAttr"))
        val attr3 = Attribute(mockedHash, encBase64Str(validAttrInfoJson))
        val attributeList = listOf(attr1, attr2, attr3)
        mockTrustedIssuerWithAttributes(attributeList)

        testedPolicy.verify(verifiableCredential) shouldBe true
    }

    private fun mockTrustedIssuerWithAttributes(attributeList: List<Attribute>) {
        val tirRecord = TrustedIssuer(did, attributeList)
        mockkObject(TrustedIssuerClient)
        every { TrustedIssuerClient.getIssuer(any()) } returns tirRecord
    }
}
