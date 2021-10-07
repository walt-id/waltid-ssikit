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
        val ephemeralKeyService = EphemeralKeyService()
        ephemeralKeyService.cryptoService::class shouldBe SunCryptoService::class
        ephemeralKeyService.keyStore::class shouldBe InMemoryKeyStoreService::class

        val keyId = ephemeralKeyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        ephemeralKeyService.listKeys().map { it.keyId } shouldContain keyId
        ephemeralKeyService.delete(keyId.id)
        ephemeralKeyService.listKeys().map { it.keyId } shouldNotContain keyId

    }

})
