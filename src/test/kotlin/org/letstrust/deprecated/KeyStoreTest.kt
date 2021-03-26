package org.letstrust.deprecated

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Test
import org.letstrust.services.key.KeyManagementService
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
    open fun saveLoadEd25519KeysTest() {
        val keyId = kms.generateKeyPair("Ed25519")
        val keys = kms.loadKeys(keyId)!!
        assertNotNull(keys)
        assertEquals(32, keys.pair.private.encoded.size)
    }

    @Test
    open fun saveLoadSecp256k1KeysTest() {
        val keyId = kms.generateKeyPair("Secp256k1")
        val keys = kms.loadKeys(keyId)!!
        assertNotNull(keys)
        assertEquals(33, keys.pair.public.encoded.size)
        assertEquals(32, keys.pair.private.encoded.size)
    }

    @Test
    open fun saveLoadStandardKeysTest() {

        var keyId = kms.generateEcKeyPair("secp256k1")
        var keys = kms.loadKeys(keyId)!!
        assertNotNull(keys)
        assertEquals("ECDSA", keys.pair.private?.algorithm)

        keyId = kms.generateKeyPair("RSA")
        keys = kms.loadKeys(keyId)!!
        assertNotNull(keys.pair)
        assertEquals("RSA", keys.pair.private?.algorithm)
    }

    @Test
    fun listKeysTest() {
        var keyId = kms.generateEcKeyPair("secp256k1")
        kms.listKeys().forEach {
            println("key $it")
        }
    }

    @Test
    open fun deleteKeysTest() {
        val keyId = kms.generateKeyPair("Ed25519")
        var keys = kms.loadKeys(keyId)
        assertNotNull(keys)

        kms.deleteKeys(keyId)
        keys = kms.loadKeys(keyId)
        assertTrue(keys === null)
    }

    @Test
    open fun addAliasTest() {
        val keyId = kms.generateKeyPair("Ed25519")
        val testAlias = UUID.randomUUID().toString()
        kms.addAlias(keyId, testAlias)
        val k1 = kms.loadKeys(testAlias)
        assertNotNull(k1)
        val k2 = kms.loadKeys(keyId)
        assertNotNull(k2)
        println(k1.pair.private.encoded.contentToString())
        assertEquals(k2.pair.private.encoded.contentToString(), k1.pair.private.encoded.contentToString())
    }
}
