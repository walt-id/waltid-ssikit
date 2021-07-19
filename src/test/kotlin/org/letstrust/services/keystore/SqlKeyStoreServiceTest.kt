package org.letstrust.services.keystore

import org.junit.Test
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.services.key.KeyService
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SqlKeyStoreServiceTest : KeyStoreServiceTest() {

    private val sqlKeyStoreService = SqlKeyStoreService()
    private val keyService = KeyService.getService()

    @BeforeTest
    fun setUp() {
    }

    @Test
    fun addAliasSqlApiTest() {
        val keyId = keyService.generate(KeyAlgorithm.EdDSA_Ed25519)
        val alias = UUID.randomUUID().toString()
        sqlKeyStoreService.addAlias(keyId, alias)
        val k1 = sqlKeyStoreService.getKeyId(alias)
        assertNotNull(k1)
        assertEquals(keyId.id, k1)
        val k2 = sqlKeyStoreService.load(keyId.id)
        assertNotNull(k2)
        assertEquals(k1, k2.keyId.id)
    }


    // TODO refactore following test
//    @Test
//    fun saveLoadByteKeysSqlApiTest() {
//        val priv = BytePrivateKey("priv".toByteArray(), "alg")
//        val pub = BytePublicKey("pub".toByteArray(), "alg")
//        val keys = Keys(UUID.randomUUID().toString(), KeyPair(pub, priv), "dummy")
//        SqlKeyStoreService.saveKeyPair(keys)
//        val keysLoaded = SqlKeyStoreService.load(keys.keyId)
//        assertNotNull(keysLoaded)
//        assertEquals(keys.keyId, keysLoaded.keyId)
//        assertEquals("priv", String(keysLoaded.pair.private.encoded))
//        assertEquals("pub", String(keysLoaded.pair.public.encoded))
//        assertEquals("alg", keysLoaded.algorithm)
//        assertEquals("byte", keysLoaded.pair.private.format)
//    }


}
