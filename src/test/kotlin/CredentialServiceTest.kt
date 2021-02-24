import foundation.identity.jsonld.JsonLDObject
import foundation.identity.jsonld.JsonLDUtils
import info.weboftrust.ldsignatures.LdProof
import info.weboftrust.ldsignatures.jsonld.LDSecurityContexts
import info.weboftrust.ldsignatures.signer.Ed25519Signature2018LdSigner
import info.weboftrust.ldsignatures.suites.SignatureSuites
import info.weboftrust.ldsignatures.verifier.Ed25519Signature2018LdVerifier
import junit.framework.Assert.assertEquals
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.codec.binary.Hex
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.letstrust.CredentialService
import org.letstrust.CredentialService.SignatureType.EcdsaSecp256k1Signature2019
import org.letstrust.CredentialService.SignatureType.Ed25519Signature2018
import org.letstrust.DidService
import org.letstrust.KeyManagementService
import org.letstrust.model.VerifiableCredential
import org.letstrust.model.VerifiablePresentation
import java.io.File
import java.net.URI
import java.security.Security
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
        Security.addProvider(BouncyCastleProvider())
    }


    //TOOD FIX @ signature-ld lib: the type in the proof is an ARRAY, rather than  a "type" : [ "Ed25519Signature2018" ],

    val testEd25519PrivateKeyString = "984b589e121040156838303f107e13150be4a80fc5088ccba0b0bdc9b1d89090de8777a28f8da1a74e7a13090ed974d879bf692d001cddee16e4cc9f84b60580"

    val testEd25519PublicKeyString = "de8777a28f8da1a74e7a13090ed974d879bf692d001cddee16e4cc9f84b60580"

    @Test
    fun testSignEd25519Signature2018() {

        val testEd25519PrivateKey = Hex.decodeHex(testEd25519PrivateKeyString.toCharArray())
        val testEd25519PublicKey = Hex.decodeHex(testEd25519PublicKeyString.toCharArray())

        val jsonLdObject = JsonLDObject.fromJson(File("input.jsonld").readText())
        jsonLdObject.documentLoader = LDSecurityContexts.DOCUMENT_LOADER
        val creator = URI.create("did:sov:WRfXPg8dantKVubE3HX8pw")
        val created = JsonLDUtils.DATE_FORMAT.parse("2017-10-24T05:33:31Z")
        val domain = "example.com"
        val nonce: String? = null
        val signer = Ed25519Signature2018LdSigner(testEd25519PrivateKey)
        signer.creator = creator
        signer.created = created
        signer.domain = domain
        signer.nonce = nonce
        val ldProof: LdProof = signer.sign(jsonLdObject)
        println(ldProof.toJson(true))
        assertEquals(SignatureSuites.SIGNATURE_SUITE_ED25519SIGNATURE2018.term, ldProof.type)
        assertEquals(creator, ldProof.creator)
        assertEquals(created, ldProof.created)
        assertEquals(domain, ldProof.domain)
        assertEquals(nonce, ldProof.nonce)
        assertEquals(
            "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..5VI99nGh5wrAJRub5likTa5lLQ2Dmfiv-ByTRfd1D4WmnOSo3N1eSLemCYlXG95VY6Na-FuEHpjofI8iz8iPBQ",
            ldProof.jws
        )
        val verifier = Ed25519Signature2018LdVerifier(testEd25519PublicKey)
        val verify: Boolean = verifier.verify(jsonLdObject, ldProof)
        assertTrue(verify)
    }

    @Test
    fun signEd25519Signature2018Test() {

        val issuerDid = DidService.createDid("key")
        val domain = "example.com"
        val nonce: String? = null
        val credMap: Map<String, String> = mapOf("one" to "two")
        val cred = JSONObject(credMap).toString()

        val vc = CredentialService.sign(issuerDid, cred, Ed25519Signature2018, domain, nonce)
        assertNotNull(vc)
        println("Credential generated: $vc")

        val vcVerified = CredentialService.verify(issuerDid, vc, Ed25519Signature2018)
        assertTrue(vcVerified)
        KeyManagementService.deleteKeys(issuerDid)
    }

    @Test
    fun signEcdsaSecp256k1Signature2019Test() {

        val keyId = KeyManagementService.generateKeyPair("Secp256k1")
        val issuerDid = DidService.createDid("key")
        val domain = "example.com"
        val nonce: String? = null
        val credMap: Map<String, String> = mapOf("one" to "two")
        val cred = JSONObject(credMap).toString()

        val vc = CredentialService.sign(keyId, cred, EcdsaSecp256k1Signature2019, domain, nonce)
        assertNotNull(vc)
        println("Credential generated: $vc")

        val vcVerified = CredentialService.verify(keyId, vc, EcdsaSecp256k1Signature2019)
        assertTrue(vcVerified)
        KeyManagementService.deleteKeys(keyId)
    }

    @Test
    fun issueWorkHistoryCredential() {

        val credOffer = readCredOffer("WorkHistory")

        val issuerDid = DidService.createDid("key")
        val domain = "example.com"
        val nonce: String? = null

        val vc = CredentialService.sign(issuerDid, credOffer, Ed25519Signature2018, domain, nonce)
        assertNotNull(vc)
        println("Credential generated: $vc")

        val vcVerified = CredentialService.verify(issuerDid, vc, Ed25519Signature2018)
        assertTrue(vcVerified)
    }

    @Test
    fun issuePermanentResidentCardCredential() {

        val credOffer = readCredOffer("PermanentResidentCard")

        val keyId = KeyManagementService.generateKeyPair("Secp256k1")
        val domain = "example.com"
        val nonce: String? = null

        val vc = CredentialService.sign(keyId, credOffer, EcdsaSecp256k1Signature2019, domain, nonce)
        assertNotNull(vc)
        println("Credential generated: $vc")

        val vcVerified = CredentialService.verify(keyId, vc, EcdsaSecp256k1Signature2019)
        assertTrue(vcVerified)
        KeyManagementService.deleteKeys(keyId)
    }

    @Test
    fun issueVerifablePresentation() {

        val vcStr = readVerifiableCredential("vc-simple-example")
        val vc = Json.decodeFromString<VerifiableCredential>(vcStr)
        val vpIn = VerifiablePresentation(listOf("https://www.w3.org/2018/credentials/v1"), "id", listOf("VerifiablePresentation"), listOf(vc, vc), null)
        val vpInputStr = Json { prettyPrint = true }.encodeToString(vpIn)

        print(vpInputStr)

        val issuerDid = DidService.createDid("key")
        val domain = "example.com"
        val nonce: String? = null

        val vp = CredentialService.sign(issuerDid, vpInputStr, Ed25519Signature2018, domain, nonce)
        assertNotNull(vp)
        println("Verifiable Presentation generated: $vp")
    }

}
