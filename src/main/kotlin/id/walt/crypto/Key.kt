package id.walt.crypto

import com.google.crypto.tink.KeysetHandle
import id.walt.services.CryptoProvider
import id.walt.services.keystore.TinkKeyStoreService
import kotlinx.serialization.Serializable
import java.security.KeyPair
import java.security.PublicKey
import java.security.interfaces.ECPublicKey

@Serializable
data class KeyId(val id: String) { // TODO Make value class (performance)
    override fun toString() = id
}

data class Key(val keyId: KeyId, val algorithm: KeyAlgorithm, val cryptoProvider: CryptoProvider) {
    fun getPublicKey(): PublicKey = when {
        keyPair != null -> keyPair!!.public
        keysetHandle != null -> TinkKeyStoreService().loadPublicKey(this) as ECPublicKey
        else -> throw Exception("No public key for $keyId")
    }

    constructor(keyId: KeyId, algorithm: KeyAlgorithm, cryptoProvider: CryptoProvider, keyPair: KeyPair) : this(
        keyId,
        algorithm,
        cryptoProvider
    ) {
        this.keyPair = keyPair
    }

    constructor(
        keyId: KeyId,
        algorithm: KeyAlgorithm,
        cryptoProvider: CryptoProvider,
        keysetHandle: KeysetHandle
    ) : this(keyId, algorithm, cryptoProvider) {
        this.keysetHandle = keysetHandle
    }

    override fun toString(): String = "Key[${keyId.id}; Algo: ${algorithm.name}; Provider: ${cryptoProvider.name}]"

    var keyPair: KeyPair? = null
    var keysetHandle: KeysetHandle? = null
}
