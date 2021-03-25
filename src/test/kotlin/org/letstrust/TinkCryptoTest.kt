package org.letstrust

import com.google.crypto.tink.config.TinkConfig
import org.junit.Test
import org.letstrust.crypto.TinkCryptoService
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TinkCryptoTest {

    init {
        TinkConfig.register()
    }

    @Test
    fun genTest() {
        val keyId = TinkCryptoService.generateKey(KeyAlgorithm.Secp256k1)
        assertNotNull(keyId.id)
    }

    @Test
    fun signTest() {
        val keyId = TinkCryptoService.generateKey(KeyAlgorithm.Secp256k1)
        val sig = TinkCryptoService.sign(keyId, "some data".toByteArray())
        val res = TinkCryptoService.verfiy(keyId, sig)
        assertTrue(res)
    }
}
