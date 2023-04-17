package id.walt.auditor

import id.walt.auditor.policies.EbsiTrustedIssuerRegistryPolicy
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.crypto.encBase64Str
import id.walt.model.Attribute
import id.walt.model.DidMethod
import id.walt.model.TrustedIssuer
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.ecosystems.essif.TrustedIssuerClient
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockkObject
import org.junit.jupiter.api.assertAll

@Ignored
class TrustedIssuerRegistryPolicyTest : AnnotationSpec() {

    private val defaultRegistry = "${TrustedIssuerClient.domain}/${TrustedIssuerClient.trustedIssuerPath}"
    private val otherRegistry = "http://my-other-registry.org/v3/issuers/"

    private val simplePolicy = EbsiTrustedIssuerRegistryPolicy();
    private val parameterizedPolicy = EbsiTrustedIssuerRegistryPolicy(otherRegistry);

    private val mockedHash = "mockHash"
    private val validAttrInfoJson =
        "{\"@context\":\"https://ebsi.eu\",\"type\":\"attribute\",\"name\":\"issuer\",\"data\":\"5d50b3fa18dde32b384d8c6d096869de\"}"
    private lateinit var did: String
    private lateinit var verifiableCredential: VerifiableCredential

    override suspend fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)

        ServiceMatrix("service-matrix.properties")

        val signatory = Signatory.getService()

        did = DidService.create(DidMethod.key)
        val didDoc = DidService.load(did)
        val vm = didDoc.assertionMethod!!.first().id

        println("Generated: $did")

        val vcStr = signatory.issue(
            "VerifiableDiploma", ProofConfig(
                issuerDid = did,
                subjectDid = did,
                issuerVerificationMethod = vm,
                proofPurpose = "Testing",
                proofType = ProofType.LD_PROOF
            )
        )

        verifiableCredential = vcStr.toVerifiableCredential()
    }

    @Test
    fun whenTrustedIssuerRegistryContainsValidAttributeThenReturnTrue() {
        val attributeList = listOf(Attribute(mockedHash, encBase64Str(validAttrInfoJson)))
        val capture = mockTrustedIssuerWithAttributes(attributeList)

        assertAll(
            { simplePolicy.verify(verifiableCredential).isSuccess },
            { capture.captured shouldBe defaultRegistry }
        )

        assertAll(
            { parameterizedPolicy.verify(verifiableCredential).isSuccess },
            { capture.captured shouldBe otherRegistry }
        )
    }


    @Test
    fun whenTrustedIssuerRegistryContainsInvalidBase64AttributeThenReturnFalse() {
        val attributeList = listOf(Attribute(mockedHash, "invalidBase64EncodedString"))
        mockTrustedIssuerWithAttributes(attributeList)

        assertAll(
            { simplePolicy.verify(verifiableCredential).isFailure },
            { parameterizedPolicy.verify(verifiableCredential).isFailure }
        )
    }

    @Test
    fun whenTrustedIssuerRegistryContainsMultipleAttributesWithLastValidThenReturnTrue() {
        val attr1 = Attribute(mockedHash, "invalidBase64EncodedString")
        val attr2 = Attribute(mockedHash, encBase64Str("invalidAttr"))
        val attr3 = Attribute(mockedHash, encBase64Str(validAttrInfoJson))
        val attributeList = listOf(attr1, attr2, attr3)
        val capture = mockTrustedIssuerWithAttributes(attributeList)

        assertAll(
            { simplePolicy.verify(verifiableCredential).isSuccess },
            { capture.captured shouldBe defaultRegistry }
        )

        assertAll(
            { parameterizedPolicy.verify(verifiableCredential).isSuccess },
            { capture.captured shouldBe otherRegistry }
        )

    }

    private fun mockTrustedIssuerWithAttributes(attributeList: List<Attribute>): CapturingSlot<String> {
        val tirRecord = TrustedIssuer(did, attributeList)
        val slot = CapturingSlot<String>()
        mockkObject(TrustedIssuerClient)
        every { TrustedIssuerClient.getIssuer(any(), capture(slot)) } returns tirRecord
        return slot
    }
}
