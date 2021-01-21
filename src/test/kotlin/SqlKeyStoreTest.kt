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
    fun saveLoadByteKeysSqlApiTest() {
        var keys = Keys(UUID.randomUUID().toString(), "priv".toByteArray(), "pub".toByteArray(), "dummy", "dummy")
        SqlKeyStore.saveKeyPair(keys)
        var keysLoaded = SqlKeyStore.loadKeyPair(keys.keyId)
        assertNotNull(keysLoaded)
        assertEquals(keys.keyId, keysLoaded.keyId)
        assertEquals("priv", String(keysLoaded.privateKey!!))
        assertEquals("pub", String(keysLoaded.publicKey!!))
    }


    @Test
    fun addAliasSqlApiTest() {
        var keyId = kms.generateEd25519KeyPair()
        SqlKeyStore.addAlias(keyId, "test-alias")
        var k1 = SqlKeyStore.getKeyId("test-alias")
        assertNotNull(k1)
        assertEquals(keyId, k1)
        var k2 = SqlKeyStore.getKeyId(keyId)
        assertNotNull(k2)
        assertEquals(k1, k2)
    }
}
