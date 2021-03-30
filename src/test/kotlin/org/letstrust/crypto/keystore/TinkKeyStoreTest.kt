package org.letstrust.crypto.keystore

import com.google.crypto.tink.config.TinkConfig
import org.junit.Test
import org.letstrust.KeyAlgorithm
import org.letstrust.crypto.TinkCryptoService

class TinkKeyStoreTest {

    init {
        TinkConfig.register()
    }

    @Test
    fun storeTest() {
        val keyId = TinkCryptoService.generateKey(KeyAlgorithm.ECDSA_Secp256k1)

        val key = TinkKeyStore.load(keyId.id)

        println(key)
    }
}
