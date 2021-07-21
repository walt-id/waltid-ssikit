package org.letstrust.crypto

import com.google.crypto.tink.config.TinkConfig
import org.junit.Test
import org.letstrust.services.crypto.TinkCryptoService
import kotlin.test.assertTrue

class TinkCryptoTest {

    init {
        TinkConfig.register()
    }

    val tinkCryptoService = TinkCryptoService()
    val data = "some data".toByteArray()

    @Test
    fun signSecp256k1Test() {
        val keyId = tinkCryptoService.generateKey(KeyAlgorithm.ECDSA_Secp256k1)
        val sig = tinkCryptoService.sign(keyId, data)
        val res = tinkCryptoService.verify(keyId, sig, data)
        assertTrue(res)
    }

    @Test
    fun signEd25519Test() {
        val keyId = tinkCryptoService.generateKey(KeyAlgorithm.EdDSA_Ed25519)
        val sig = tinkCryptoService.sign(keyId, data)
        val res = tinkCryptoService.verify(keyId, sig, data)
        assertTrue(res)
    }
}
