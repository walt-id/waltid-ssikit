package org.letstrust.deprecated

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.crypto.SunCryptoService
import org.letstrust.crypto.keystore.FileSystemKeyStore
import org.letstrust.crypto.keystore.SqlKeyStore
import org.letstrust.services.key.KeyManagementService
import kotlin.test.assertTrue

@Deprecated(message = "We probably remove FileSystemKeyStore at some point")
open class FileSystemKeyStoreTest {//: KeyStoreTest() {

    @Before
    fun setUp() {
        SunCryptoService.setKeyStore(FileSystemKeyStore)
        KeyManagementService.setKeyStore(FileSystemKeyStore)
    }

    @After
    fun tearDown() {
        SunCryptoService.setKeyStore(SqlKeyStore)
        KeyManagementService.setKeyStore(SqlKeyStore)
    }

    @Test
    fun listKeysTest() {

        var keyId1 = KeyManagementService.generate(KeyAlgorithm.EdDSA_Ed25519)
        var keyId2 = KeyManagementService.generate(KeyAlgorithm.ECDSA_Secp256k1)

        var foundKeyId1 = false
        var foundKeyId2 = false
        KeyManagementService.listKeys().forEach {
            println("key $it")
            if (keyId1.equals(it.keyId)) foundKeyId1 = true
            if (keyId2.equals(it.keyId)) foundKeyId2 = true
        }
        assertTrue(foundKeyId1)
        assertTrue(foundKeyId2)
    }
}
