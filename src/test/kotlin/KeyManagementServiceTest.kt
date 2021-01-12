import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Before
import org.junit.Test
import java.security.KeyFactory
import java.security.Security
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class KeyManagementServiceTest {

    @Before
    fun setup() {
        Security.addProvider(BouncyCastleProvider())
    }

    @Test
    fun checkRequiredAlgorithms() {
        val kms = KeyManagementService
        var secp256k1 = false
        var P521 = false
        kms.getSupportedCurveNames().forEach {
            // println(it)
            if ("secp256k1".equals(it)) {
                secp256k1 = true
            } else if ("P-521".equals(it)) {
                P521 = true
            }
        }
        assertTrue(secp256k1)
        assertTrue(P521)
    }

    @Test
    fun generateSecp256k1KeyPairTest() {
        val kms = KeyManagementService
        val keyId = kms.generateEcKeyPair("secp256k1")
        val keysLoaded = kms.loadKeys(keyId)
        assertEquals(keyId, keysLoaded?.keyId)
        assertNotNull(keysLoaded?.pair)
        assertNotNull(keysLoaded?.pair?.private)
        assertNotNull(keysLoaded?.pair?.public)
        assertEquals("ECDSA", keysLoaded?.pair?.private?.algorithm)
        kms.deleteKeys(keyId)
    }

    @Test
    fun generateEd25519KeyPairTest() {
        val kms = KeyManagementService
        val keyId = kms.generateEd25519KeyPair()
        val keysLoaded = kms.loadKeys(keyId)
        assertEquals(keyId, keysLoaded?.keyId)
        assertNotNull(keysLoaded?.privateKey)
        assertNotNull(keysLoaded?.publicKey)
        var pubKey = keysLoaded?.publicKey
        assertEquals(32, pubKey?.size)
        assertTrue(kms.getBase58PublicKey(keyId).length > 32)
        kms.deleteKeys(keyId)
    }

    @Test
    fun generateRsaKeyPairTest() {
        val kms = KeyManagementService
        val ks = FileSystemKeyStore
        ks.updateProvider(KeyFactory.getInstance("RSA", "BC"))
        val keyId = kms.generateRsaKeyPair()
        val keysLoaded = kms.loadKeys(keyId)
        assertEquals(keyId, keysLoaded?.keyId)
        assertNotNull(keysLoaded?.pair)
        assertNotNull(keysLoaded?.pair?.private)
        assertNotNull(keysLoaded?.pair?.public)
        assertEquals("RSA", keysLoaded?.pair?.private?.algorithm)
        kms.deleteKeys(keyId)
    }

}
