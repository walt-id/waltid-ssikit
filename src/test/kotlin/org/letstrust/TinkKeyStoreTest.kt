package org.letstrust

import com.google.crypto.tink.config.TinkConfig
import org.junit.Test
import org.letstrust.crypto.TinkCryptoService

class TinkKeyStoreTest {

    init {
        TinkConfig.register()
    }

    @Test
    fun storeTest() {
        val key = TinkCryptoService.generateKey(KeyAlgorithm.Secp256k1)
    }
}
