package org.letstrust.crypto.keystore

import org.junit.Test
import org.letstrust.KeyAlgorithm
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
    fun addAliasSqlApiTest() {
        val keyId = kms.generate(KeyAlgorithm.EdDSA_Ed25519)
        val alias = UUID.randomUUID().toString()
        SqlKeyStore.addAlias(keyId, alias)
        val k1 = SqlKeyStore.getKeyId(alias)
        assertNotNull(k1)
        assertEquals(keyId.id, k1)
        val k2 = SqlKeyStore.load(keyId.id)
        assertNotNull(k2)
        assertEquals(k1, k2.keyId.id)
    }


    // TODO refactore following test
//    @Test
//    fun saveLoadByteKeysSqlApiTest() {
//        val priv = BytePrivateKey("priv".toByteArray(), "alg")
//        val pub = BytePublicKey("pub".toByteArray(), "alg")
//        val keys = Keys(UUID.randomUUID().toString(), KeyPair(pub, priv), "dummy")
//        SqlKeyStore.saveKeyPair(keys)
//        val keysLoaded = SqlKeyStore.load(keys.keyId)
//        assertNotNull(keysLoaded)
//        assertEquals(keys.keyId, keysLoaded.keyId)
//        assertEquals("priv", String(keysLoaded.pair.private.encoded))
//        assertEquals("pub", String(keysLoaded.pair.public.encoded))
//        assertEquals("alg", keysLoaded.algorithm)
//        assertEquals("byte", keysLoaded.pair.private.format)
//    }


}
