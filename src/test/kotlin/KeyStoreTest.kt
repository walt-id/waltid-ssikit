import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Test
import java.security.KeyFactory
import java.security.Security
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


        //TODO algo-info should come from the keys metadata
        KeyFactory.getInstance("RSA", "BC")
        var skeyId = kms.generateRsaKeyPair()
        var skeys = kms.loadKeys(skeyId)!!
        assertNotNull(skeys.pair)
        assertEquals("RSA", skeys.pair?.private?.algorithm)




        var keyId = kms.generateEd25519KeyPair()
        var keys = kms.loadKeys(keyId)!!
        assertNotNull(keys)
        assertEquals(32, keys.privateKey?.size)

        keyId = kms.generateSecp256k1KeyPair()
        keys = kms.loadKeys(keyId)!!
        assertNotNull(keys)
        assertEquals(32, keys.privateKey?.size)

        keyId = kms.generateEcKeyPair("secp256k1")
        keys = kms.loadKeys(keyId)!!
        assertNotNull(keys)
        assertEquals("ECDSA", keys.pair!!.private.algorithm)


    }

    @Test
    open fun saveLoadStandardKeysTest() {
        //TODO algo-info should come from the keys metadata
        // FileSystemKeyStore.updateProvider(KeyFactory.getInstance("ECDSA", "BC"))
        var keyId = kms.generateEcKeyPair("secp256k1")
        var keys = kms.loadKeys(keyId)!!
        assertNotNull(keys)
        assertEquals("ECDSA", keys.pair!!.private.algorithm)

        //TODO algo-info should come from the keys metadata
        //FileSystemKeyStore.updateProvider(KeyFactory.getInstance("RSA", "BC"))
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
        kms.addAlias(keyId, "test-alias")
        var k1 = kms.loadKeys("test-alias")
        assertNotNull(k1)
        var k2 = kms.loadKeys(keyId)
        assertNotNull(k2)
        println(k1.privateKey.contentToString())
        assertEquals(k2.privateKey.contentToString(), k1.privateKey.contentToString())
    }
}
