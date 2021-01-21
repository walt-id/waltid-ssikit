import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Test
import java.security.Security
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

open class KeyStoreTest {

    val kms = KeyManagementService

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    @Test
    open fun saveLoadByteKeysTest() {
        var keyId = kms.generateEd25519KeyPair()
        var keys = kms.loadKeys(keyId)!!
        assertNotNull(keys)
        assertEquals(32, keys.privateKey?.size)

        keyId = kms.generateSecp256k1KeyPair()
        keys = kms.loadKeys(keyId)!!
        assertNotNull(keys)
        assertEquals(32, keys.privateKey?.size)
    }

    @Test
    open fun saveLoadStandardKeysTest() {

        var keyId = kms.generateEcKeyPair("secp256k1")
        var keys = kms.loadKeys(keyId)!!
        assertNotNull(keys)
        assertEquals("ECDSA", keys.pair?.private?.algorithm)

        keyId = kms.generateRsaKeyPair()
        keys = kms.loadKeys(keyId)!!
        assertNotNull(keys.pair)
        assertEquals("RSA", keys.pair?.private?.algorithm)
    }

    @Test
    open fun deleteKeysTest() {
        var keyId = kms.generateEd25519KeyPair()
        var keys = kms.loadKeys(keyId)
        assertNotNull(keys)

        kms.deleteKeys(keyId)
        keys = kms.loadKeys(keyId)
        assertTrue(keys === null)
    }

    @Test
    open fun addAliasTest() {
        var keyId = kms.generateEd25519KeyPair()
        var testAlias = UUID.randomUUID().toString()
        kms.addAlias(keyId, testAlias)
        var k1 = kms.loadKeys(testAlias)
        assertNotNull(k1)
        var k2 = kms.loadKeys(keyId)
        assertNotNull(k2)
        println(k1.privateKey.contentToString())
        assertEquals(k2.privateKey.contentToString(), k1.privateKey.contentToString())
    }
}
