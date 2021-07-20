package org.letstrust.crypto.keystore

import id.walt.servicematrix.ServiceMatrix
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.services.crypto.SunCryptoService
import org.letstrust.services.key.KeyService
import org.letstrust.services.keystore.FileSystemKeyStoreService
import org.letstrust.services.keystore.SqlKeyStoreService
import kotlin.test.assertEquals

open class FileSystemKeyStoreTest {//: KeyStoreTest() {

    val sunCryptoService = SunCryptoService()
    val keyService = KeyService.getService()
    val fileSystemKeyStoreService = FileSystemKeyStoreService()
    val sqlKeyStoreService = SqlKeyStoreService()

    @Before
    fun setUp() {
        ServiceMatrix("service-matrix.properties")
        sunCryptoService.setKeyStore(fileSystemKeyStoreService)
    }

    @After
    fun tearDown() {
        sunCryptoService.setKeyStore(sqlKeyStoreService)
    }

    @Test
    fun listKeysTest() {

        var keyId1 = keyService.generate(KeyAlgorithm.EdDSA_Ed25519)
        var keyId2 = keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        var key1 = fileSystemKeyStoreService.load(keyId1.id)
        var key2 = fileSystemKeyStoreService.load(keyId2.id)
        assertEquals(keyId1, key1.keyId)
        assertEquals(keyId2, key2.keyId)
    }
}
