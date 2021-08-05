package id.walt.services.crypto

import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.KeyId

class CustomCryptoService : CryptoService() {
    override fun generateKey(algorithm: KeyAlgorithm): KeyId {
        TODO("Not yet implemented")
    }

    override fun sign(keyId: KeyId, data: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

    override fun verify(keyId: KeyId, sig: ByteArray, data: ByteArray): Boolean {
        TODO("Not yet implemented")
    }

    override fun encrypt(
        keyId: KeyId,
        algorithm: String,
        plainText: ByteArray,
        authData: ByteArray?,
        iv: ByteArray?
    ): ByteArray {
        TODO("Not yet implemented")
    }

    override fun decrypt(
        keyId: KeyId,
        algorithm: String,
        plainText: ByteArray,
        authData: ByteArray?,
        iv: ByteArray?
    ): ByteArray {
        TODO("Not yet implemented")
    }
}
