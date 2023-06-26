package id.walt.services.did

import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.KeyId
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.crypto.CryptoService
import id.walt.services.key.KeyService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class DidKeyCreationTest : StringSpec({
    ServiceMatrix("service-matrix.properties")

    val cryptoService = CryptoService.getService()
    val keyService = KeyService.getService()

    fun createAndLoadDid(key: KeyId, options: DidOptions? = null) = let {
        val did = DidService.create(DidMethod.key, key.id, options)
        println("Created: $did")

        val loaded = DidService.load(did)
        println("Loaded: $loaded")
        loaded
    }

    "Create default did:key" {
        val did = DidService.create(DidMethod.key)
        println("Created: $did")

        val loaded = DidService.load(did)
        println("Loaded: $loaded")
    }

    "Create EdDSA_Ed25519 did:key" {
        val key = cryptoService.generateKey(KeyAlgorithm.EdDSA_Ed25519)
        createAndLoadDid(key)
    }

    "Create ECDSA_Secp256r1 did:key" {
        val key = cryptoService.generateKey(KeyAlgorithm.ECDSA_Secp256r1)
        createAndLoadDid(key)
    }
    "Create ECDSA_Secp256k1 did:key" {
        val key = cryptoService.generateKey(KeyAlgorithm.ECDSA_Secp256k1)
        createAndLoadDid(key)
    }

    "Create RSA did:key" {
        val key = cryptoService.generateKey(KeyAlgorithm.RSA)
        createAndLoadDid(key)
    }

    "Create jwk_jcs-pub did:key" {
        val jwkPubKey = "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"ngy44T1vxAT6Di4nr-UaM9K3Tlnz9pkoksDokKFkmNc\",\"y\":\"QCRfOKlSM31GTkb4JHx3nXB4G_jSPMsbdjzlkT_UpPc\"}"
        val keyId = keyService.importKey(jwkPubKey)
        val result = createAndLoadDid(keyId, DidKeyCreateOptions(useJwkJcsPub = true))
        result.id shouldBe "did:key:z2dmzD81cgPx8Vki7JbuuMmFYrWPgYoytykUZ3eyqht1j9KbsEYvdrjxMjQ4tpnje9BDBTzuNDP3knn6qLZErzd4bJ5go2CChoPjd5GAH3zpFJP5fuwSk66U5Pq6EhF4nKnHzDnznEP8fX99nZGgwbAh1o7Gj1X52Tdhf7U4KTk66xsA5r"
    }
})
