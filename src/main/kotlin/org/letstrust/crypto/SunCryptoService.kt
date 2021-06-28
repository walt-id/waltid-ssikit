package org.letstrust.crypto

import com.nimbusds.jose.crypto.impl.AESGCM
import com.nimbusds.jose.jwk.ECKey
import org.letstrust.CryptoProvider
import org.letstrust.LetsTrustServices
import org.letstrust.crypto.keystore.KeyStore
import org.web3j.crypto.ECDSASignature
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Hash
import java.security.SecureRandom
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SunCryptoService : CryptoService {

    private const val RSA_KEY_SIZE = 4096

    private var ks: KeyStore = LetsTrustServices.load<KeyStore>()

    var ecJWK: ECKey? = null

    internal fun setKeyStore(ks: KeyStore) {
        SunCryptoService.ks = ks
    }

    override fun generateKey(algorithm: KeyAlgorithm): KeyId {

        val generator = when (algorithm) {
            KeyAlgorithm.ECDSA_Secp256k1 -> keyPairGeneratorSecp256k1()
            KeyAlgorithm.EdDSA_Ed25519 -> keyPairGeneratorEd25519()
        }

        val keyPair = generator.generateKeyPair()
        val key = Key(newKeyId(), algorithm, CryptoProvider.SUN, keyPair)
        ks.store(key)
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

    fun verify(keyId: String, signature: ByteArray) {

    }

    override fun sign(keyId: KeyId, data: ByteArray): ByteArray {
        val key = ks.load(keyId.id, true)
        val sig = when (key.algorithm) {
            KeyAlgorithm.ECDSA_Secp256k1 -> Signature.getInstance("SHA256withECDSA")
            KeyAlgorithm.EdDSA_Ed25519 -> Signature.getInstance("Ed25519")
        }
        sig.initSign(key.keyPair!!.private)
        sig.update(data)
        return sig.sign()
    }

    override fun verify(keyId: KeyId, sig: ByteArray, data: ByteArray): Boolean {
        val key = ks.load(keyId.id)
        val signature = when (key.algorithm) {
            KeyAlgorithm.ECDSA_Secp256k1 -> Signature.getInstance("SHA256withECDSA")
            KeyAlgorithm.EdDSA_Ed25519 -> Signature.getInstance("Ed25519")
        }
        signature.initVerify(key.keyPair!!.public)
        signature.update(data)
        return signature.verify(sig)
    }

    var secretKey: SecretKey? = null
    override fun encrypt(keyId: KeyId, algorithm: String, plainText: ByteArray, authData: ByteArray?, iv: ByteArray?): ByteArray {

        //TODO: load key
        val keyGenerator: KeyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256, SecureRandom());
        secretKey = keyGenerator.generateKey()

        val c = Cipher.getInstance(algorithm)
        c.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(AESGCM.AUTH_TAG_BIT_LENGTH, iv))
        authData?.let { c.updateAAD(authData) }
        return c.doFinal(plainText)
    }

    override fun decrypt(keyId: KeyId, algorithm: String, plainText: ByteArray, authData: ByteArray?, iv: ByteArray?): ByteArray {
        val c = Cipher.getInstance(algorithm)
        c.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(AESGCM.AUTH_TAG_BIT_LENGTH, iv))
        authData?.let { c.updateAAD(authData) }
        return c.doFinal(plainText)
    }

    override fun signWithECDSA(keyId: KeyId, data: ByteArray): ECDSASignature {
        val key = ks.load(keyId.id, true)
        when (key.algorithm) {
            KeyAlgorithm.ECDSA_Secp256k1 -> return ECKeyPair.create(key.keyPair).sign(Hash.sha3(data))
            else -> throw IllegalArgumentException("Wrong key algorithm: secp256k1 is required.")
        }
    }
}
