package id.walt.services.keystore

import com.google.crypto.tink.config.TinkConfig
import io.kotest.core.spec.style.AnnotationSpec
import id.walt.crypto.KeyAlgorithm
import id.walt.services.crypto.TinkCryptoService

class TinkKeyStoreServiceTest : AnnotationSpec() {

    init {
        TinkConfig.register()
    }

    val tinkCryptoService = TinkCryptoService()
    val tinkKeyStoreService = TinkKeyStoreService()

    @Test
    fun storeTest() {
        val keyId = tinkCryptoService.generateKey(KeyAlgorithm.ECDSA_Secp256k1)
        val key = tinkKeyStoreService.load(keyId.id)

        println(key)
    }
}
