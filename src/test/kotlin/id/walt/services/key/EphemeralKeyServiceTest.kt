package id.walt.services.key

import id.walt.crypto.KeyAlgorithm
import id.walt.services.crypto.SunCryptoService
import id.walt.services.keystore.InMemoryKeyStoreService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

class EphemeralKeyServiceTest : StringSpec({
    "Crypto and Keystore services do not need config and are in memory" {
        EphemeralKeyService.getService().cryptoService::class shouldBe SunCryptoService::class
        EphemeralKeyService.getService().keyStore::class shouldBe InMemoryKeyStoreService::class

        val keyId = EphemeralKeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1)
        EphemeralKeyService.getService().listKeys().map { it.keyId } shouldContain keyId
        EphemeralKeyService.getService().delete(keyId.id)
        EphemeralKeyService.getService().listKeys().map { it.keyId } shouldNotContain keyId

    }

})
