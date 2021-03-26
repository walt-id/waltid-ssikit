package org.letstrust.crypto

import com.google.crypto.tink.KeysetHandle
import org.letstrust.CryptoProvider
import org.letstrust.KeyAlgorithm
import java.security.KeyPair
import java.util.*

inline class KeyId(val id: String) {
    constructor() : this("LetsTrust-Key-${UUID.randomUUID().toString().replace("-", "")}")
}

data class Key(val keyId: KeyId, val algorithm: KeyAlgorithm, val cryptoProvider: CryptoProvider) {
    constructor(keyId: KeyId, algorithm: KeyAlgorithm, cryptoProvider: CryptoProvider, keyPair: KeyPair) : this(keyId, algorithm, cryptoProvider) {
        this.keyPair = keyPair
    }

    constructor(keyId: KeyId, algorithm: KeyAlgorithm, cryptoProvider: CryptoProvider, keysetHandle: KeysetHandle) : this(keyId, algorithm, cryptoProvider) {
        this.keysetHandle = keysetHandle
    }

    var keyPair: KeyPair? = null
    var keysetHandle: KeysetHandle? = null
}
