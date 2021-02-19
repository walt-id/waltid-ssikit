import org.junit.Test
import org.letstrust.BytePrivateKey
import org.letstrust.BytePublicKey
import org.letstrust.Keys
import org.letstrust.SqlKeyStore
import java.security.KeyPair
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
        val priv = BytePrivateKey("priv".toByteArray(), "alg")
        val pub = BytePublicKey("pub".toByteArray(), "alg")
        val keys = Keys(UUID.randomUUID().toString(), KeyPair(pub, priv), "dummy")
        SqlKeyStore.saveKeyPair(keys)
        val keysLoaded = SqlKeyStore.loadKeyPair(keys.keyId)
        assertNotNull(keysLoaded)
        assertEquals(keys.keyId, keysLoaded.keyId)
        assertEquals("priv", String(keysLoaded.pair.private.encoded))
        assertEquals("pub", String(keysLoaded.pair.public.encoded))
        assertEquals("alg", keysLoaded.algorithm)
        assertEquals("byte", keysLoaded.pair.private.format)
    }

    @Test
    fun addAliasSqlApiTest() {
        val keyId = kms.generateKeyPair("Ed25519")
        val alias = UUID.randomUUID().toString()
        SqlKeyStore.addAlias(keyId, alias)
        val k1 = SqlKeyStore.getKeyId(alias)
        assertNotNull(k1)
        assertEquals(keyId, k1)
        val k2 = SqlKeyStore.getKeyId(keyId)
        assertNotNull(k2)
        assertEquals(k1, k2)
    }
}
