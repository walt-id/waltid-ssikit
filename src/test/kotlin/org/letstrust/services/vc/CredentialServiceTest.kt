package org.letstrust.services.vc

import id.walt.servicematrix.ServiceMatrix
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import org.letstrust.model.DidMethod
import org.letstrust.model.VerifiableCredential
import org.letstrust.model.VerifiablePresentation
import org.letstrust.model.encodePretty
import org.letstrust.services.did.DidService
import org.letstrust.test.getTemplate
import org.letstrust.test.readCredOffer
import org.letstrust.vclib.vcs.*
import java.io.File
import java.sql.Timestamp
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CredentialServiceTest {

    private val credentialService = VCService.getService()


    val VC_PATH = "src/test/resources/verifiable-credentials"

    @Before
    fun setup() {
        ServiceMatrix("service-matrix.properties")
    }

    fun genericSignVerify(issuerDid: String, credOffer: String) {

        val vcStr = credentialService.sign(issuerDid, credOffer)
        println("Credential generated: $vcStr")

        val vc = VC.decode(vcStr)
        println("Credential decoded: $vc")

        val vcVerified = credentialService.verify(vcStr)
        assertTrue(vcVerified.verified)
        assertEquals(VerificationType.VERIFIABLE_CREDENTIAL, vcVerified.verificationType)

        val vpStr = credentialService.present(vcStr, "domain.com", "nonce")
        println("Presentation generated: $vpStr")

        // TODO FIX
//        val vp = VC.decode(vpStr)
//        println(vp)
//        assertEquals("domain.com", vp.proof?.domain)
//        assertEquals("nonce", vp.proof?.nonce)

        val vpVerified = credentialService.verify(vpStr)
        assertTrue(vpVerified.verified)
        assertEquals(VerificationType.VERIFIABLE_PRESENTATION, vpVerified.verificationType)
    }

    @Test
    fun signEbsiVerifiableAttestation() {
        val template = getTemplate("ebsi-attestation") as EbsiVerifiableAttestation

        val issuerDid = DidService.create(DidMethod.web)

        template.issuer = issuerDid
        template.credentialSubject!!.id = issuerDid // self signed

        val credOffer = Json.encodeToString(template)

        genericSignVerify(issuerDid, credOffer)
    }

    @Test
    fun signEuropass() {
        val template = getTemplate("europass") as Europass

        val issuerDid = DidService.create(DidMethod.key)

        template.issuer = issuerDid
        template.credentialSubject!!.id = issuerDid // self signed
        template.learningAchievement!!.title!!.text!!.text = "Some Europass specific title"

        val credOffer = Json.encodeToString(template)

        genericSignVerify(issuerDid, credOffer)
    }

    @Test
    fun signPermanentResitentCard() {
        val template = getTemplate("permanent-resident-card") as PermanentResidentCard

        val issuerDid = DidService.create(DidMethod.key)

        template.issuer = issuerDid
        template.credentialSubject!!.id = issuerDid // self signed
        template.identifier = "some-prc-specific-id"

        val credOffer = Json.encodeToString(template)

        genericSignVerify(issuerDid, credOffer)
    }

    // TODO: create DID for holder @Test
    fun presentVa() {
        val vaStr = File("$VC_PATH/vc-ebsi-verifiable-authorisation.json").readText()

        val vp = credentialService.present(vaStr, null, null)

        println(vp)
    }

    @Test
    fun presentEuropassTest() {

        val issuerDid = DidService.create(DidMethod.ebsi)
        val subjectDid = DidService.create(DidMethod.key)
        val domain = "example.com"
        val challenge: String? = "asdf"

        val template = getTemplate("europass") as Europass

        template.issuer = issuerDid
        template.credentialSubject!!.id = subjectDid
        template.learningAchievement!!.title!!.text!!.text = "Some Europass specific title"

        val vc = credentialService.sign(issuerDid, template.encode())

        val vcSigned = VC.decode(vc)
        println(vcSigned)

        val vp = credentialService.present(vc, domain, challenge)
        println("Presentation generated: $vp")

        val vpVerified = credentialService.verifyVp(vp)
        assertTrue(vpVerified)

    }


    // TODO: consider methods below, as old data-model might be used
    @Test
    fun signCredentialECDSASecp256k1Test() {

        val credOffer = readCredOffer("WorkHistory")

        val issuerDid = DidService.create(DidMethod.web) // DID web uses an ECDSA Secp256k1
        val domain = "example.com"
        val nonce: String? = null

        val vc = credentialService.sign(issuerDid, credOffer, domain, nonce)
        assertNotNull(vc)
        println("Credential generated: $vc")

        val vcVerified = credentialService.verifyVc(issuerDid, vc)
        assertTrue(vcVerified)
    }

    @Test
    fun signCredentialEd25519k1Test() {

        val credOffer = readCredOffer("WorkHistory")

        val issuerDid = DidService.create(DidMethod.key) // DID key uses an EdDSA Ed25519k1 key
        val domain = "example.com"
        val nonce: String? = null

        val vc = credentialService.sign(issuerDid, credOffer, domain, nonce)
        assertNotNull(vc)
        println("Credential generated: $vc")

        val vcVerified = credentialService.verifyVc(issuerDid, vc)
        assertTrue(vcVerified)
    }

    @Test
    fun signEuropassCredentialTest() {

        val credOffer = readCredOffer("VerifiableAttestation-Europass")

        val issuerDid = DidService.create(DidMethod.key) // DID key uses an EdDSA Ed25519k1 key
        val domain = "example.com"
        val nonce: String? = null

        val vc = credentialService.sign(issuerDid, credOffer, domain, nonce)
        assertNotNull(vc)
        println("Credential generated: $vc")

        val vcVerified = credentialService.verifyVc(issuerDid, vc)
        assertTrue(vcVerified)
    }


    @Test
    fun signCredentialWrongValidationKeyTest() {

        val credOffer = readCredOffer("WorkHistory")

        val issuerDid = DidService.create(DidMethod.key)
        val anotherDid = DidService.create(DidMethod.key)

        val vc = credentialService.sign(issuerDid, credOffer)
        assertNotNull(vc)
        println("Credential generated: $vc")

        val vcVerified = credentialService.verifyVc(anotherDid, vc)
        assertFalse(vcVerified)
    }

    @Test
    fun signCredentialInvalidDataTest() {

        val credOffer = readCredOffer("vc-offer-simple-example")
        val issuerDid = DidService.create(DidMethod.key)

        val vcStr = credentialService.sign(issuerDid, credOffer)
        println("Credential generated: $vcStr")
        val vcInvalid = Json.decodeFromString<VerifiableCredential>(vcStr)
        vcInvalid.id = "INVALID ID"
        val vcInvalidStr = vcInvalid.encodePretty()
        println("Credential generated: ${vcInvalidStr}")

        val vcVerified = credentialService.verifyVc(issuerDid, vcInvalidStr)
        assertFalse(vcVerified)
    }

    // TODO @Test
    fun presentValidCredentialTest() {

        val credOffer = Json.decodeFromString<VerifiableCredential>(readCredOffer("vc-offer-simple-example"))
        val issuerDid = DidService.create(DidMethod.web)
        val subjectDid = DidService.create(DidMethod.key)

        credOffer.id = Timestamp.valueOf(LocalDateTime.now()).time.toString()
        credOffer.issuer = issuerDid
        credOffer.credentialSubject.id = subjectDid

        credOffer.issuanceDate = LocalDateTime.now()

        val vcReqEnc = Json { prettyPrint = true }.encodeToString(credOffer)

        println("Credential request:\n$vcReqEnc")

        val vcStr = credentialService.sign(issuerDid, vcReqEnc)
        val vc = Json.decodeFromString<VerifiableCredential>(vcStr)
        println("Credential generated: ${vc.encodePretty()}")

        val vpIn = VerifiablePresentation(
            listOf("https://www.w3.org/2018/credentials/v1"),
            "id",
            listOf("VerifiablePresentation"),
            listOf(vc),
            null
        )
        val vpInputStr = Json { prettyPrint = true }.encodeToString(vpIn)

        val domain = "example.com"
        val nonce: String? = "asdf"
        val vp = credentialService.sign(issuerDid, vpInputStr, domain, nonce)
        assertNotNull(vp)
        println("Verifiable Presentation generated: $vp")

        var ret = credentialService.verifyVp(vp)
        assertTrue { ret }
    }

    //TODO @Test
    fun presentInvalidCredentialTest() {
        val issuerDid = DidService.create(DidMethod.web)
        val subjectDid = DidService.create(DidMethod.key)

        val credOffer = Json.decodeFromString<VerifiableCredential>(readCredOffer("vc-offer-simple-example"))
        //      credOffer.id = Timestamp.valueOf(LocalDateTime.now()).time.toString() // This line is causing LD-Signatures to fail (will produces a valid signature, although the data is invalidated below)
        credOffer.issuer = issuerDid
        credOffer.credentialSubject.id = subjectDid

        credOffer.issuanceDate = LocalDateTime.now()

        //val vcReqEnc = readCredOffer("vc-offer-simple-example") -> produces false-signature for invalid credential
        val vcReqEnc = Json {
            prettyPrint = true
        }.encodeToString(credOffer) // FIXME does not produce false-signature for invalid credential

        println("Credential request:\n$vcReqEnc")

        val vcStr = credentialService.sign(issuerDid, vcReqEnc)
        println("Credential generated: $vcStr")
        val vcInvalid = Json.decodeFromString<VerifiableCredential>(vcStr)
        vcInvalid.credentialSubject.id = "INVALID ID"
        val vcInvalidStr = vcInvalid.encodePretty()
        println("Credential generated: ${vcInvalidStr}")

        val vcValid = credentialService.verifyVc(issuerDid, vcInvalidStr)
        assertFalse(vcValid)

        val vcVerified = credentialService.verifyVc(issuerDid, vcInvalidStr)

        val vpIn = VerifiablePresentation(
            listOf("https://www.w3.org/2018/credentials/v1"),
            "id",
            listOf("VerifiablePresentation"),
            listOf(vcInvalid),
            null
        )
        val vpInputStr = Json { prettyPrint = true }.encodeToString(vpIn)

        print(vpInputStr)

        val domain = "example.com"
        val nonce: String? = "asdf"
        val vp = credentialService.sign(issuerDid, vpInputStr, domain, nonce)
        assertNotNull(vp)
        println("Verifiable Presentation generated: $vp")

        var ret = credentialService.verifyVp(vp)
        assertFalse(ret)
    }
}
