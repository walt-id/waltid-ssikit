package id.walt.services.crypto

import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.KeyId
import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import org.web3j.crypto.ECDSASignature

abstract class CryptoService : WaltIdService() {
    override val implementation get() = serviceImplementation<CryptoService>()

    open fun generateKey(algorithm: KeyAlgorithm): KeyId = implementation.generateKey(algorithm)
    open fun sign(keyId: KeyId, data: ByteArray): ByteArray = implementation.sign(keyId, data)
    open fun verify(keyId: KeyId, sig: ByteArray, data: ByteArray): Boolean = implementation.verify(keyId, sig, data)
    open fun encrypt(
        keyId: KeyId,
        algorithm: String,
        plainText: ByteArray,
        authData: ByteArray?,
        iv: ByteArray?
    ): ByteArray = implementation.encrypt(keyId, algorithm, plainText, authData, iv)

    open fun decrypt(
        keyId: KeyId,
        algorithm: String,
        plainText: ByteArray,
        authData: ByteArray?,
        iv: ByteArray?
    ): ByteArray = implementation.decrypt(keyId, algorithm, plainText, authData, iv)

    open fun signEthTransaction(keyId: KeyId, encodedTx: ByteArray): ECDSASignature =
        implementation.signEthTransaction(keyId, encodedTx)

    companion object : ServiceProvider {
        override fun getService() = object : CryptoService() {}
        override fun defaultImplementation() = SunCryptoService()
    }
}

