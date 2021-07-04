package org.letstrust.crypto.keystore

import com.google.crypto.tink.config.TinkConfig
import org.junit.Test
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.services.crypto.TinkCryptoService

class TinkKeyStoreTest {

    init {
        TinkConfig.register()
    }

    val tinkCryptoService = TinkCryptoService()

    @Test
    fun storeTest() {
        val keyId = tinkCryptoService.generateKey(KeyAlgorithm.ECDSA_Secp256k1)
        val key = TinkKeyStore.load(keyId.id)

        println(key)
    }
}
