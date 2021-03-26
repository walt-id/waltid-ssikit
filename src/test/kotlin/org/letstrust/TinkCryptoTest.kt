package org.letstrust

import com.google.crypto.tink.config.TinkConfig
import org.junit.Test
import org.letstrust.crypto.TinkCryptoService
import kotlin.test.assertTrue

class TinkCryptoTest {

    init {
        TinkConfig.register()
    }

    @Test
    fun signSecp256k1Test() {
        val data = "some data".toByteArray()
        val keyId = TinkCryptoService.generateKey(KeyAlgorithm.Secp256k1)
        val sig = TinkCryptoService.sign(keyId, data)
        val res = TinkCryptoService.verfiy(keyId, sig, data)
        assertTrue(res)
    }

    @Test
    fun signEd25519Test() {
        val data = "some data".toByteArray()
        val keyId = TinkCryptoService.generateKey(KeyAlgorithm.Ed25519)
        val sig = TinkCryptoService.sign(keyId, data)
        val res = TinkCryptoService.verfiy(keyId, sig, data)
        assertTrue(res)
    }
}
