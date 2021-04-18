package org.letstrust.crypto

import org.junit.Test
import kotlin.test.assertTrue

class SunCryptoTest {

    val crypto = SunCryptoService
    val data = "some data".toByteArray()

    @Test
    fun signSecp256k1Test() {
        val keyId = crypto.generateKey(KeyAlgorithm.ECDSA_Secp256k1)
        val sig = crypto.sign(keyId, data)
        val res = crypto.verify(keyId, sig, data)
        assertTrue(res)
    }

    // Ed25519 only supported with Java 15
    @Test
    fun signEd25519Test() {
        val keyId = crypto.generateKey(KeyAlgorithm.EdDSA_Ed25519)
        val sig = crypto.sign(keyId, data)
        val res = crypto.verify(keyId, sig, data)
        assertTrue(res)
    }
}
