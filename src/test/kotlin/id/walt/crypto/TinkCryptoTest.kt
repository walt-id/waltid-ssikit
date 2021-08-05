package id.walt.crypto

import com.google.crypto.tink.config.TinkConfig
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import id.walt.services.crypto.TinkCryptoService

class TinkCryptoTest : AnnotationSpec() {

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
        res shouldBe true
    }

    @Test
    fun signEd25519Test() {
        val keyId = tinkCryptoService.generateKey(KeyAlgorithm.EdDSA_Ed25519)
        val sig = tinkCryptoService.sign(keyId, data)
        val res = tinkCryptoService.verify(keyId, sig, data)
        res shouldBe true
    }
}
