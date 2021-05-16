package org.letstrust.services.vc

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
import org.letstrust.test.readCredOffer
import java.sql.Timestamp
import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CredentialServiceTest {

    @Before
    fun setup() {

    }

    @Test
    fun signCredentialECDSASecp256k1Test() {

        val credOffer = readCredOffer("WorkHistory")

        val issuerDid = DidService.create(DidMethod.web) // DID web uses an ECDSA Secp256k1
        val domain = "example.com"
        val nonce: String? = null

        val vc = CredentialService.sign(issuerDid, credOffer, domain, nonce)
        assertNotNull(vc)
        println("Credential generated: $vc")

        val vcVerified = CredentialService.verifyVc(issuerDid, vc)
        assertTrue(vcVerified)
    }

    @Test
    fun signCredentialEd25519k1Test() {

        val credOffer = readCredOffer("WorkHistory")

        val issuerDid = DidService.create(DidMethod.key) // DID key uses an EdDSA Ed25519k1 key
        val domain = "example.com"
        val nonce: String? = null

        val vc = CredentialService.sign(issuerDid, credOffer, domain, nonce)
        assertNotNull(vc)
        println("Credential generated: $vc")

        val vcVerified = CredentialService.verifyVc(issuerDid, vc)
        assertTrue(vcVerified)
    }

    @Test
    fun signEuropassCredentialTest() {

        val credOffer = readCredOffer("VerifiableAttestation-Europass")

        val issuerDid = DidService.create(DidMethod.key) // DID key uses an EdDSA Ed25519k1 key
        val domain = "example.com"
        val nonce: String? = null

        val vc = CredentialService.sign(issuerDid, credOffer, domain, nonce)
        assertNotNull(vc)
        println("Credential generated: $vc")

        val vcVerified = CredentialService.verifyVc(issuerDid, vc)
        assertTrue(vcVerified)
    }

    @Test
    fun signCredentialWrongValidationKeyTest() {

        val credOffer = readCredOffer("WorkHistory")

        val issuerDid = DidService.create(DidMethod.key)
        val anotherDid = DidService.create(DidMethod.key)

        val vc = CredentialService.sign(issuerDid, credOffer)
        assertNotNull(vc)
        println("Credential generated: $vc")

        val vcVerified = CredentialService.verifyVc(anotherDid, vc)
        assertFalse(vcVerified)
    }

    @Test
    fun signCredentialInvalidDataTest() {

        val credOffer = readCredOffer("vc-offer-simple-example")
        val issuerDid = DidService.create(DidMethod.key)

        val vcStr = CredentialService.sign(issuerDid, credOffer)
        println("Credential generated: $vcStr")
        val vcInvalid = Json.decodeFromString<VerifiableCredential>(vcStr)
        vcInvalid.id = "INVALID ID"
        val vcInvalidStr = vcInvalid.encodePretty()
        println("Credential generated: ${vcInvalidStr}")

        val vcVerified = CredentialService.verifyVc(issuerDid, vcInvalidStr)
        assertFalse(vcVerified)
    }

    @Test
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

        val vcStr = CredentialService.sign(issuerDid, vcReqEnc)
        val vc = Json.decodeFromString<VerifiableCredential>(vcStr)
        println("Credential generated: ${vc.encodePretty()}")

        val vpIn = VerifiablePresentation(listOf("https://www.w3.org/2018/credentials/v1"), "id", listOf("VerifiablePresentation"), listOf(vc), null)
        val vpInputStr = Json { prettyPrint = true }.encodeToString(vpIn)

        val domain = "example.com"
        val nonce: String? = "asdf"
        val vp = CredentialService.sign(issuerDid, vpInputStr, domain, nonce)
        assertNotNull(vp)
        println("Verifiable Presentation generated: $vp")

        var ret = CredentialService.verifyVp(vp)
        assertTrue { ret }
    }

    @Test
    fun presentInvalidCredentialTest() {
        val issuerDid = DidService.create(DidMethod.web)
        val subjectDid = DidService.create(DidMethod.key)

        val credOffer = Json.decodeFromString<VerifiableCredential>(readCredOffer("vc-offer-simple-example"))
        //      credOffer.id = Timestamp.valueOf(LocalDateTime.now()).time.toString() // This line is causing LD-Signatures to fail (will produces a valid signature, although the data is invalidated below)
        credOffer.issuer = issuerDid
        credOffer.credentialSubject.id = subjectDid

        credOffer.issuanceDate = LocalDateTime.now()

        //val vcReqEnc = readCredOffer("vc-offer-simple-example") -> produces false-signature for invalid credential
        val vcReqEnc = Json { prettyPrint = true }.encodeToString(credOffer) // FIXXX does not produce false-signature for invalid credential

        println("Credential request:\n$vcReqEnc")

        val vcStr = CredentialService.sign(issuerDid, vcReqEnc)
        println("Credential generated: $vcStr")
        val vcInvalid = Json.decodeFromString<VerifiableCredential>(vcStr)
        vcInvalid.credentialSubject.id = "INVALID ID"
        val vcInvalidStr = vcInvalid.encodePretty()
        println("Credential generated: ${vcInvalidStr}")

        val vcValid = CredentialService.verifyVc(issuerDid, vcInvalidStr)
        assertFalse(vcValid)

        val vcVerified = CredentialService.verifyVc(issuerDid, vcInvalidStr)

        val vpIn = VerifiablePresentation(listOf("https://www.w3.org/2018/credentials/v1"), "id", listOf("VerifiablePresentation"), listOf(vcInvalid), null)
        val vpInputStr = Json { prettyPrint = true }.encodeToString(vpIn)

        print(vpInputStr)

        val domain = "example.com"
        val nonce: String? = "asdf"
        val vp = CredentialService.sign(issuerDid, vpInputStr, domain, nonce)
        assertNotNull(vp)
        println("Verifiable Presentation generated: $vp")

        var ret = CredentialService.verifyVp(vp)
        assertFalse(ret)
    }
}
