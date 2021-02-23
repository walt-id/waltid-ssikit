import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
