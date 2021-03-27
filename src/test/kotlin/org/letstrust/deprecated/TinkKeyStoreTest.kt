package org.letstrust.deprecated

import com.google.crypto.tink.config.TinkConfig
import org.junit.Test
import org.letstrust.KeyAlgorithm
import org.letstrust.crypto.TinkCryptoService
import org.letstrust.services.key.TinkKeyStore

class TinkKeyStoreTest {

    init {
        TinkConfig.register()
    }

    @Test
    fun storeTest() {
        val keyId = TinkCryptoService.generateKey(KeyAlgorithm.Secp256k1)

        val key = TinkKeyStore.load(keyId)

        println(key)
    }
}
