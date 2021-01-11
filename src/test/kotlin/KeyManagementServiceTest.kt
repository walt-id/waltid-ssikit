import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Before
import org.junit.Test
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
    fun generateEcKeyPairsTest() {

        var kms = KeyManagementService

        kms.getSupportedCurveNames().forEach {
            val keyId = kms.generateEcKeyPair(it)
            val keysLoaded = kms.loadKeys(keyId)
            assertEquals(keyId, keysLoaded?.keyId)
            assertNotNull(keysLoaded?.pair)
            assertNotNull(keysLoaded?.pair?.private)
            assertNotNull(keysLoaded?.pair?.public)
            assertEquals("ECDSA", keysLoaded?.pair?.private?.algorithm)
            kms.deleteKeys(keyId)
        }
    }

    @Test
    fun generateEC25519KeyPairTest() {

        var kms = KeyManagementService

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

}
