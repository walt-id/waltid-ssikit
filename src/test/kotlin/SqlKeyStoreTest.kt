import org.junit.Test
import java.security.KeyFactory
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SqlKeyStoreTest : KeyStoreTest() {

    @BeforeTest
    fun setUp() {
        kms.setKeyStore(SqlKeyStore)
    }

    @Test
    fun saveLoadDummyByteKeysTest() {
        var keys = Keys(UUID.randomUUID().toString(), "priv".toByteArray(), "pub".toByteArray())
        SqlKeyStore.saveKeyPair(keys)
        var keysLoaded = SqlKeyStore.loadKeyPair(keys.keyId)
        assertNotNull(keysLoaded)
        assertEquals(keys.keyId, keysLoaded.keyId)
        assertEquals("priv", String(keysLoaded.privateKey!!))
        assertEquals("pub", String(keysLoaded.publicKey!!))
    }


    @Test
    override fun addAliasTest() {
        var keyId = kms.generateEd25519KeyPair()
        SqlKeyStore.addAlias(keyId, "test-alias")
        var k1 = SqlKeyStore.getKeyId("test-alias")
        assertNotNull(k1)
    }

    @Test
    override fun saveLoadByteKeysTest() {

        var keyId = kms.generateEd25519KeyPair()
        var keys = kms.loadKeys(keyId)
        assertNotNull(keys)
        assertEquals(32, keys.privateKey?.size)

        keyId = kms.generateSecp256k1KeyPair()
        keys = kms.loadKeys(keyId)!!
        assertNotNull(keys)
        assertEquals(32, keys.privateKey?.size)

    }

}
