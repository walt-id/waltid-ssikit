package org.letstrust.crypto

import com.google.crypto.tink.KeysetHandle
import org.letstrust.CryptoProvider
import org.letstrust.KeyAlgorithm
import org.letstrust.services.key.TinkKeyStore
import java.security.KeyPair
import java.security.interfaces.ECPublicKey

inline class KeyId(val id: String) {}

data class Key(val keyId: KeyId, val algorithm: KeyAlgorithm, val cryptoProvider: CryptoProvider) {
    fun getPublicKey(): ECPublicKey {
        if (this.keyPair != null){
            return this.keyPair!!.public as ECPublicKey
        } else if (this.keysetHandle != null) {
            return TinkKeyStore.loadPublicKey(this) as ECPublicKey
        }
        throw Exception("No public key for $keyId")
    }

    constructor(keyId: KeyId, algorithm: KeyAlgorithm, cryptoProvider: CryptoProvider, keyPair: KeyPair) : this(keyId, algorithm, cryptoProvider) {
        this.keyPair = keyPair
    }

    constructor(keyId: KeyId, algorithm: KeyAlgorithm, cryptoProvider: CryptoProvider, keysetHandle: KeysetHandle) : this(keyId, algorithm, cryptoProvider) {
        this.keysetHandle = keysetHandle
    }

    var keyPair: KeyPair? = null
    var keysetHandle: KeysetHandle? = null
}

data class KeyMetaData(val algorithm: KeyAlgorithm, val cryptoProvider: CryptoProvider)
