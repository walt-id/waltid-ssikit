package org.letstrust.services.keystore

import id.walt.servicematrix.ServiceMatrix
import id.walt.servicematrix.ServiceRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.services.crypto.SunCryptoService
import org.letstrust.services.key.KeyService
import kotlin.test.assertEquals

open class FileSystemKeyStoreServiceTest {//: KeyStoreServiceTest() {

    private val sunCryptoService = SunCryptoService()
    private val fileSystemKeyStoreService = FileSystemKeyStoreService()
    private val sqlKeyStoreService = SqlKeyStoreService()
    private val keyService = KeyService.getService()

    @Before
    fun setUp() {
        ServiceMatrix("service-matrix.properties")
        ServiceRegistry.registerService<KeyStoreService>(FileSystemKeyStoreService())
        sunCryptoService.setKeyStore(fileSystemKeyStoreService)
    }

    @After
    fun tearDown() {
        sunCryptoService.setKeyStore(sqlKeyStoreService)
    }

    @Test
    fun listKeysTest() {
        println("Generating keys...")
        val keyId1 = keyService.generate(KeyAlgorithm.EdDSA_Ed25519)
        println("Generated EdDSA_Ed25519 key: $keyId1")
        val keyId2 = keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        println("Generated ECDSA_Secp256k1 key: $keyId2")

        println("Loading keys...")

        println("Loading EdDSA_Ed25519 key: $keyId1")
        val key1 = fileSystemKeyStoreService.load(keyId1.id)

        println("Generated ECDSA_Secp256k1 key: $keyId2")
        val key2 = fileSystemKeyStoreService.load(keyId2.id)

        assertEquals(keyId1, key1.keyId)
        assertEquals(keyId2, key2.keyId)
    }
}
