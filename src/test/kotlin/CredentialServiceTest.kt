import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.letstrust.CredentialService
import org.letstrust.CredentialService.SignatureType.EcdsaSecp256k1Signature2019
import org.letstrust.CredentialService.SignatureType.Ed25519Signature2018
import org.letstrust.DidService
import org.letstrust.KeyManagementService
import java.io.File
import java.security.Security
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CredentialServiceTest {

    private val RESOURCES_PATH: String = "src/test/resources"

    fun readCredOffer(fileName: String) =
        File("$RESOURCES_PATH/verifiable-credentials/${fileName}.json").readText(Charsets.UTF_8)

    @Before
    fun setup() {
        Security.addProvider(BouncyCastleProvider())
    }

    @Test
    fun signEd25519Signature2018Test() {

        val issuerDid = DidService.createDidKey()
        val domain = "example.com"
        val nonce: String? = null
        val credMap: Map<String, String> = mapOf("one" to "two")
        val cred = JSONObject(credMap).toString()

        val vc = CredentialService.sign(issuerDid, domain, nonce, cred, Ed25519Signature2018)
        assertNotNull(vc)
        println("Credential generated: $vc")

        val vcVerified = CredentialService.verify(issuerDid, vc, Ed25519Signature2018)
        assertTrue(vcVerified)
        KeyManagementService.deleteKeys(issuerDid)
    }

    @Test
    fun signEcdsaSecp256k1Signature2019Test() {

        val keyId = KeyManagementService.generateKeyPair("Secp256k1")
        val issuerDid = DidService.createDidKey()
        val domain = "example.com"
        val nonce: String? = null
        val credMap: Map<String, String> = mapOf("one" to "two")
        val cred = JSONObject(credMap).toString()

        val vc = CredentialService.sign(keyId, domain, nonce, cred, EcdsaSecp256k1Signature2019)
        assertNotNull(vc)
        println("Credential generated: $vc")

        val vcVerified = CredentialService.verify(keyId, vc, EcdsaSecp256k1Signature2019)
        assertTrue(vcVerified)
        KeyManagementService.deleteKeys(keyId)
    }

    @Test
    fun issueWorkHistoryCredential() {

        val credOffer = readCredOffer("WorkHistory")

        val issuerDid = DidService.createDidKey()
        val domain = "example.com"
        val nonce: String? = null

        val vc = CredentialService.sign(issuerDid, domain, nonce, credOffer, Ed25519Signature2018)
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

        val vc = CredentialService.sign(keyId, domain, nonce, credOffer, EcdsaSecp256k1Signature2019)
        assertNotNull(vc)
        println("Credential generated: $vc")

        val vcVerified = CredentialService.verify(keyId, vc, EcdsaSecp256k1Signature2019)
        assertTrue(vcVerified)
        KeyManagementService.deleteKeys(keyId)
    }

}
