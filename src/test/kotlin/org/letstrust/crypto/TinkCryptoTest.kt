package org.letstrust.crypto

import com.google.crypto.tink.config.TinkConfig
import org.junit.Test
import kotlin.test.assertTrue

class TinkCryptoTest {

    init {
        TinkConfig.register()
    }

    val crypto = TinkCryptoService
    val data = "some data".toByteArray()

    @Test
    fun signSecp256k1Test() {
        val keyId = crypto.generateKey(KeyAlgorithm.ECDSA_Secp256k1)
        val sig = crypto.sign(keyId, data)
        val res = crypto.verfiy(keyId, sig, data)
        assertTrue(res)
    }

    @Test
    fun signEd25519Test() {
        val keyId = crypto.generateKey(KeyAlgorithm.EdDSA_Ed25519)
        val sig = crypto.sign(keyId, data)
        val res = crypto.verfiy(keyId, sig, data)
        assertTrue(res)
    }
}
