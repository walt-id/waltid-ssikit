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
import java.io.File
import java.sql.Timestamp
import java.time.LocalDateTime
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CredentialServiceTest {

    private val RESOURCES_PATH: String = "src/test/resources"

    fun readCredOffer(fileName: String) =
        File("$RESOURCES_PATH/verifiable-credentials/${fileName}.json").readText(Charsets.UTF_8)

    fun readVerifiableCredential(fileName: String) =
        File("$RESOURCES_PATH/verifiable-credentials/${fileName}.json").readText(Charsets.UTF_8)

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

        val vcVerified = CredentialService.verify(issuerDid, vc)
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

        val vcVerified = CredentialService.verify(issuerDid, vc)
        assertTrue(vcVerified)
    }

    @Test
    fun presentCredentialTest() {

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

        print(vpInputStr)

        val domain = "example.com"
        val nonce: String? = "asdf"
        val vp = CredentialService.sign(issuerDid, vpInputStr, domain, nonce)
        assertNotNull(vp)
        println("Verifiable Presentation generated: $vp")

        var ret = CredentialService.verifyVp(vp)
        assertTrue { ret }
    }
}
