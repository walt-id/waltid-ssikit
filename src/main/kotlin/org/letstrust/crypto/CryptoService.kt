package org.letstrust.crypto


interface CryptoService {

    fun generateKey(algorithm: KeyAlgorithm): KeyId
    fun sign(keyId: KeyId, data: ByteArray): ByteArray
    fun verify(keyId: KeyId, sig: ByteArray, data: ByteArray): Boolean
    fun encrypt(keyId: KeyId, algorithm: String, plainText: ByteArray, authData: ByteArray?, iv: ByteArray?): ByteArray
    fun decrypt(keyId: KeyId, algorithm: String, plainText: ByteArray, authData: ByteArray?, iv: ByteArray?): ByteArray
}



