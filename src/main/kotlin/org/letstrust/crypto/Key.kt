package org.letstrust.crypto

import com.google.crypto.tink.KeysetHandle
import kotlinx.serialization.Serializable
import org.letstrust.CryptoProvider
import org.letstrust.crypto.keystore.TinkKeyStore
import java.security.KeyPair
import java.security.PublicKey
import java.security.interfaces.ECPublicKey

@Serializable
data class KeyId(val id: String) {
    override fun toString() = id
}

data class Key(val keyId: KeyId, val algorithm: KeyAlgorithm, val cryptoProvider: CryptoProvider) {
    fun getPublicKey(): PublicKey = when {
        keyPair != null -> keyPair!!.public
        keysetHandle != null -> TinkKeyStore.loadPublicKey(this) as ECPublicKey
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

    var keyPair: KeyPair? = null
    var keysetHandle: KeysetHandle? = null
}
