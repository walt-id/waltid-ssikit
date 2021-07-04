package org.letstrust.crypto

import org.junit.Test
import org.letstrust.services.crypto.SunCryptoService
import kotlin.test.assertTrue

class SunCryptoTest {

    val sunCryptoService = SunCryptoService()
    val data = "some data".toByteArray()

    @Test
    fun signSecp256k1Test() {
        val keyId = sunCryptoService.generateKey(KeyAlgorithm.ECDSA_Secp256k1)
        val sig = sunCryptoService.sign(keyId, data)
        val res = sunCryptoService.verify(keyId, sig, data)
        assertTrue(res)
    }

    // Ed25519 only supported with Java 15
    @Test
    fun signEd25519Test() {
        val keyId = sunCryptoService.generateKey(KeyAlgorithm.EdDSA_Ed25519)
        val sig = sunCryptoService.sign(keyId, data)
        val res = sunCryptoService.verify(keyId, sig, data)
        assertTrue(res)
    }
}
