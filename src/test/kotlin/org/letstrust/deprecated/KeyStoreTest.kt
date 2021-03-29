package org.letstrust.deprecated

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Test
import org.letstrust.KeyAlgorithm
import org.letstrust.services.key.FileSystemKeyStore
import org.letstrust.services.key.KeyManagementService
import java.security.Security
import java.util.*
import kotlin.reflect.full.createInstance
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

open class KeyStoreTest {

    val kms = KeyManagementService

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    @Test
    fun serviceLoaderTest() {
        val loader = ServiceLoader.load(org.letstrust.services.key.KeyStore::class.java)
        val ksServiceLoader = loader.iterator().next()
        println(ksServiceLoader)

        val ksKClass = Class.forName("org.letstrust.services.key.CustomKeyStore").kotlin.createInstance()
        println(ksKClass)

        val ksObject = FileSystemKeyStore
        println(ksObject)

    }


    @Test
    open fun addAliasTest() {
        val keyId = kms.generate(KeyAlgorithm.EdDSA_Ed25519)
        val testAlias = UUID.randomUUID().toString()
        kms.addAlias(keyId, testAlias)
        val k1 = kms.load(testAlias)
        assertNotNull(k1)
        val k2 = kms.load(keyId.id)
        assertNotNull(k2)
        assertEquals(k2.getPublicKey().encoded.contentToString(), k1.getPublicKey().encoded.contentToString())
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

        kms.delete(keyId)
        keys = kms.loadKeys(keyId)
        assertTrue(keys === null)
    }


}
