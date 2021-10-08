package id.walt.services.keystore

import id.walt.crypto.Key
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.keyPairGeneratorSecp256k1
import id.walt.crypto.newKeyId
import id.walt.services.CryptoProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class InMemoryKeyStoreServiceTest : StringSpec({

    val inMemoryKeyStore = InMemoryKeyStoreService()
    val inMemoryKeyStore2 = InMemoryKeyStoreService()
    val key = Key(
        newKeyId(),
        KeyAlgorithm.ECDSA_Secp256k1,
        CryptoProvider.SUN,
        keyPairGeneratorSecp256k1().generateKeyPair()
    )
    val alias = "my-test-alias"

    cleanUpStore(inMemoryKeyStore)

    "In memory key store can store keys, add aliases, list, get and delete keys by id" {
        inMemoryKeyStore.store(key)
        inMemoryKeyStore.listKeys() shouldBe listOf(key)
        inMemoryKeyStore.load(key.keyId.id) shouldBe key

        inMemoryKeyStore.addAlias(key.keyId, alias)
        inMemoryKeyStore.listKeys() shouldBe listOf(key)
        inMemoryKeyStore.getKeyId(alias) shouldBe key.keyId.id
        inMemoryKeyStore.load(alias) shouldBe key

        inMemoryKeyStore.delete(key.keyId.id)
        inMemoryKeyStore.listKeys() shouldBe emptyList()
        inMemoryKeyStore.getKeyId(key.keyId.id) shouldBe null
        inMemoryKeyStore.getKeyId(alias) shouldBe null
        shouldThrow<NullPointerException> { inMemoryKeyStore.load(key.keyId.id) }
        shouldThrow<NullPointerException> { inMemoryKeyStore.load(alias) }
    }

    "All InMemoryKeyStoreService instances reference the same HKVStore" {
        inMemoryKeyStore.store(key)
        inMemoryKeyStore2.listKeys() shouldBe listOf(key)
    }
})

fun cleanUpStore(inMemoryKeyStore: KeyStoreService) {
    inMemoryKeyStore.listKeys().forEach { inMemoryKeyStore.delete(it.keyId.id) }
}
