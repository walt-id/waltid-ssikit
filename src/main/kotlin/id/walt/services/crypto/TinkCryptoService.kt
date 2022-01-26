package id.walt.services.crypto

import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.PublicKeySign
import com.google.crypto.tink.PublicKeyVerify
import id.walt.crypto.Key
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.KeyId
import id.walt.crypto.newKeyId
import id.walt.services.CryptoProvider
import id.walt.services.keystore.KeyStoreService
import id.walt.services.keystore.TinkKeyStoreService
import org.web3j.crypto.ECDSASignature


open class TinkCryptoService : CryptoService() {
    private var keyStore: KeyStoreService = TinkKeyStoreService() //KeyStoreService.getService()
    override fun generateKey(algorithm: KeyAlgorithm): KeyId {
//        EcdsaSignKeyManager.createKeyTemplate(
//            HashType.SHA256,
//            EllipticCurveType.CURVE25519,
//            EcdsaSignatureEncoding.DER,
//            KeyTemplate.OutputPrefixType.RAW
//        )
        // https://github.com/google/tink/issues/146
        val keysetHandle = when (algorithm) {
            KeyAlgorithm.ECDSA_Secp256k1 -> KeysetHandle.generateNew(KeyTemplates.get("ECDSA_P256_RAW"))
            KeyAlgorithm.EdDSA_Ed25519 -> KeysetHandle.generateNew(KeyTemplates.get("ED25519_RAW"))
            else -> throw IllegalArgumentException("Key algorithm: $algorithm not supported")
        }

        val key = Key(newKeyId(), algorithm, CryptoProvider.TINK, keysetHandle)
        keyStore.store(key)
        return key.keyId
    }

    override fun sign(keyId: KeyId, data: ByteArray): ByteArray {
        val key = keyStore.load(keyId.id)
        val signer: PublicKeySign = key.keysetHandle!!.getPrimitive(PublicKeySign::class.java)
        /// JCA expects a DER encoded signature: ECDSA.transcodeSignatureToDER(signer.sign(data))
        return signer.sign(data)
    }

    override fun verify(keyId: KeyId, sig: ByteArray, data: ByteArray): Boolean {
        val key = keyStore.load(keyId.id)
        val verifier: PublicKeyVerify = key.keysetHandle!!.publicKeysetHandle.getPrimitive(PublicKeyVerify::class.java)
        try {
            verifier.verify(sig, data)
        } catch (e: Exception) {
            return false
        }
        return true
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

    override fun signEthTransaction(keyId: KeyId, encodedTx: ByteArray): ECDSASignature {
        TODO("Not yet implemented")
    }

}
