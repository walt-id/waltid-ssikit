package org.letstrust.services.keystore

import id.walt.servicematrix.ServiceMatrix
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Test
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.services.key.KeyService
import java.security.Security
import java.util.*
import kotlin.reflect.full.createInstance
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

open class KeyStoreServiceTest {

    private val keyService = KeyService.getService()

    init {
        Security.addProvider(BouncyCastleProvider())
        ServiceMatrix("service-matrix.properties")
    }

    @Test
    fun serviceLoaderTest() {
        val loader = ServiceLoader.load(KeyStoreService::class.java)
        val ksServiceLoader = loader.iterator().next()
        println(ksServiceLoader)

        val ksKClass = Class.forName("org.letstrust.services.keystore.CustomKeyStoreService").kotlin.createInstance()
        println(ksKClass)

        val ksObject = FileSystemKeyStoreService
        println(ksObject)
    }

    @Test
    open fun addAliasTest() {
        val keyId = keyService.generate(KeyAlgorithm.EdDSA_Ed25519)
        val testAlias = UUID.randomUUID().toString()
        keyService.addAlias(keyId, testAlias)
        val k1 = keyService.load(testAlias, KeyType.PRIVATE)
        assertNotNull(k1)
        val k2 = keyService.load(keyId.id, KeyType.PRIVATE)
        assertNotNull(k2)
        assertEquals(k2.getPublicKey().encoded.contentToString(), k1.getPublicKey().encoded.contentToString())
    }

    @Test
    open fun saveLoadEd25519KeysTest() {
        val keyId = keyService.generate(KeyAlgorithm.EdDSA_Ed25519)
        val key = keyService.load(keyId.id, KeyType.PRIVATE)
        assertEquals(48, key.keyPair!!.private.encoded.size)
    }

    @Test
    open fun saveLoadSecp256k1KeysTest() {
        val keyId = keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val key = keyService.load(keyId.id, KeyType.PRIVATE)
        assertEquals(88, key.keyPair!!.public.encoded.size)
        assertEquals(144, key.keyPair!!.private.encoded.size)
    }

    @Test
    fun listKeysTest() {
        var keyId = keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        keyService.listKeys().forEach {
            println("key $it")
        }
    }

    @Test
    open fun deleteKeysTest() {
        val keyId = keyService.generate(KeyAlgorithm.EdDSA_Ed25519)
        var key = keyService.load(keyId.id, KeyType.PRIVATE)
        keyService.delete(key.keyId.id)
        assertFailsWith(Exception::class, "Key was not deleted correctly", block = { keyService.load(keyId.id, KeyType.PRIVATE) })
    }
}
