package id.walt.services.crypto

import com.nimbusds.jose.crypto.impl.AESGCM
import com.nimbusds.jose.jwk.ECKey
import id.walt.crypto.*
import id.walt.services.CryptoProvider
import id.walt.services.context.ContextManager
import id.walt.services.keystore.KeyStoreService
import id.walt.services.keystore.KeyType
import org.web3j.crypto.ECDSASignature
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Hash
import java.security.SecureRandom
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

open class SunCryptoService : CryptoService() {

    private var _customKeyStore: KeyStoreService? = null
    private val keyStore: KeyStoreService
        get() = when(_customKeyStore) {
            null -> ContextManager.keyStore
            else -> _customKeyStore!!
        }

    var ecJWK: ECKey? = null

    fun setKeyStore(newKeyStore: KeyStoreService) {
        _customKeyStore = newKeyStore
    }

    override fun generateKey(algorithm: KeyAlgorithm): KeyId {

        val generator = when (algorithm) {
            KeyAlgorithm.ECDSA_Secp256k1 -> keyPairGeneratorSecp256k1()
            KeyAlgorithm.EdDSA_Ed25519 -> keyPairGeneratorEd25519()
            KeyAlgorithm.RSA -> keyPairGeneratorRsa()
        }

        val keyPair = generator.generateKeyPair()
        val key = Key(newKeyId(), algorithm, CryptoProvider.SUN, keyPair)
        keyStore.store(key)
        return key.keyId
    }

//    fun generate(): String {
//        ecJWK = ECKeyGenerator(Curve.SECP256K1)
//            .keyUse(KeyUse.SIGNATURE)
//            .keyID("123")
//            .generate()
//        return ecJWK!!.keyID
//    }

//    fun sign(keyId: String, data: ByteArray): ByteArray {
//
//        val signingKey = KeyManagementService.loadKeys(keyId)
//
//        val jcaSignature = try {
//            val dsa = ECDSA.getSignerAndVerifier(JWSAlgorithm.ES256K, null)
//            dsa.initSign(signingKey!!.toEcKey().toECPrivateKey())
//            dsa.update(data)
//            dsa.sign()
//        } catch (e: InvalidKeyException) {
//            throw JOSEException(e.message, e)
//        }
//        return jcaSignature
//    }

    /*fun verify(keyId: String, signature: ByteArray) {

    }*/

    override fun sign(keyId: KeyId, data: ByteArray): ByteArray {
        val key = keyStore.load(keyId.id, KeyType.PRIVATE)
        val signature = getSignature(key)
        signature.initSign(key.keyPair!!.private)
        signature.update(data)
        return signature.sign()
    }

    private fun getSignature(key: Key): Signature {
        val sig = when (key.algorithm) {
            KeyAlgorithm.ECDSA_Secp256k1 -> Signature.getInstance("SHA256withECDSA")
            KeyAlgorithm.EdDSA_Ed25519 -> Signature.getInstance("Ed25519")
            KeyAlgorithm.RSA -> Signature.getInstance("SHA256withRSA")
        }
        return sig
    }

    override fun verify(keyId: KeyId, sig: ByteArray, data: ByteArray): Boolean {
        val key = keyStore.load(keyId.id)
        val signature = getSignature(key)
        signature.initVerify(key.keyPair!!.public)
        signature.update(data)
        return signature.verify(sig)
    }

    var secretKey: SecretKey? = null
    override fun encrypt(
        keyId: KeyId,
        algorithm: String,
        plainText: ByteArray,
        authData: ByteArray?,
        iv: ByteArray?
    ): ByteArray {

        //TODO: load key
        val keyGenerator: KeyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256, SecureRandom())
        secretKey = keyGenerator.generateKey()

        val c = Cipher.getInstance(algorithm)
        c.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(AESGCM.AUTH_TAG_BIT_LENGTH, iv))
        authData?.let { c.updateAAD(authData) }
        return c.doFinal(plainText)
    }

    override fun decrypt(
        keyId: KeyId,
        algorithm: String,
        plainText: ByteArray,
        authData: ByteArray?,
        iv: ByteArray?
    ): ByteArray {
        val c = Cipher.getInstance(algorithm)
        c.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(AESGCM.AUTH_TAG_BIT_LENGTH, iv))
        authData?.let { c.updateAAD(authData) }
        return c.doFinal(plainText)
    }

    override fun signEthTransaction(keyId: KeyId, encodedTx: ByteArray): ECDSASignature {
        val key = keyStore.load(keyId.id, KeyType.PRIVATE)
        when (key.algorithm) {
            KeyAlgorithm.ECDSA_Secp256k1 -> return ECKeyPair.create(key.keyPair).sign(Hash.sha3(encodedTx))
            else -> throw IllegalArgumentException("Wrong key algorithm: secp256k1 is required.")
        }
    }
}
