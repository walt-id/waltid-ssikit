package org.letstrust.crypto.keystore

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Test
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.services.key.KeyManagementService
import java.security.Security
import java.util.*
import kotlin.reflect.full.createInstance
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

open class KeyStoreTest {

    val kms = KeyManagementService

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    @Test
    fun serviceLoaderTest() {
        val loader = ServiceLoader.load(KeyStore::class.java)
        val ksServiceLoader = loader.iterator().next()
        println(ksServiceLoader)

        val ksKClass = Class.forName("org.letstrust.crypto.keystore.CustomKeyStore").kotlin.createInstance()
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
        val keyId = kms.generate(KeyAlgorithm.EdDSA_Ed25519)
        val key = kms.load(keyId.id)!!
        assertEquals(48, key.keyPair!!.private.encoded.size)
    }

    @Test
    open fun saveLoadSecp256k1KeysTest() {
        val keyId = kms.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val key = kms.load(keyId.id)!!
        assertEquals(88, key.keyPair!!.public.encoded.size)
        assertEquals(144, key.keyPair!!.private.encoded.size)
    }

    @Test
    fun listKeysTest() {
        var keyId = kms.generate(KeyAlgorithm.ECDSA_Secp256k1)
        kms.listKeys().forEach {
            println("key $it")
        }
    }

    @Test
    open fun deleteKeysTest() {
        val keyId = kms.generate(KeyAlgorithm.EdDSA_Ed25519)
        var key = kms.load(keyId.id)
        kms.delete(key.keyId.id)
        assertFailsWith(Exception::class, "Key was not deleted correctly", block = { kms.load(keyId.id) })
    }
}
