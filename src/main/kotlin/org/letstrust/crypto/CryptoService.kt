package org.letstrust.crypto

import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.PublicKeySign
import com.google.crypto.tink.PublicKeyVerify
import com.google.crypto.tink.signature.EcdsaSignKeyManager
import com.google.crypto.tink.signature.Ed25519PrivateKeyManager
import com.nimbusds.jose.crypto.impl.AESGCM
import com.nimbusds.jose.jwk.ECKey
import org.letstrust.*
import org.letstrust.crypto.keystore.KeyStore
import org.letstrust.crypto.keystore.TinkKeyStore
import java.security.SecureRandom
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec


interface CryptoService {

    fun generateKey(algorithm: KeyAlgorithm): KeyId
    fun sign(keyId: KeyId, data: ByteArray): ByteArray
    fun verfiy(keyId: KeyId, sig: ByteArray, data: ByteArray): Boolean
    fun encrypt(keyId: KeyId, algorithm: String, plainText: ByteArray, authData: ByteArray?, iv: ByteArray?): ByteArray
    fun decrypt(keyId: KeyId, algorithm: String, plainText: ByteArray, authData: ByteArray?, iv: ByteArray?): ByteArray
}


object TinkCryptoService : CryptoService {
    private var ks: KeyStore = TinkKeyStore //LetsTrustServices.load<KeyStore>()
    override fun generateKey(algorithm: KeyAlgorithm): KeyId {
//        EcdsaSignKeyManager.createKeyTemplate(
//            HashType.SHA256,
//            EllipticCurveType.CURVE25519,
//            EcdsaSignatureEncoding.DER,
//            KeyTemplate.OutputPrefixType.RAW
//        )
        // https://github.com/google/tink/issues/146
        val keysetHandle = when (algorithm) {
            KeyAlgorithm.ECDSA_Secp256k1 -> KeysetHandle.generateNew(EcdsaSignKeyManager.rawEcdsaP256Template())
            KeyAlgorithm.EdDSA_Ed25519 -> KeysetHandle.generateNew(Ed25519PrivateKeyManager.rawEd25519Template())
        }

        println(keysetHandle)

        val key = Key(newKeyId(), algorithm, CryptoProvider.TINK, keysetHandle)
        ks.store(key)
        return key.keyId
    }

    override fun sign(keyId: KeyId, data: ByteArray): ByteArray {
        val key = ks.load(keyId.id)
        val signer: PublicKeySign = key.keysetHandle!!.getPrimitive(PublicKeySign::class.java)
        /// JCA expectes a DER encoded signature: ECDSA.transcodeSignatureToDER(signer.sign(data))
        return signer.sign(data)
    }

    override fun verfiy(keyId: KeyId, sig: ByteArray, data: ByteArray): Boolean {
        val key = ks.load(keyId.id)!!
        val verifier: PublicKeyVerify = key.keysetHandle!!.publicKeysetHandle.getPrimitive(PublicKeyVerify::class.java)
        try {
            verifier.verify(sig, data);
        } catch (e: Exception) {
            return false
        }
        return true
    }

    override fun encrypt(keyId: KeyId, algorithm: String, plainText: ByteArray, authData: ByteArray?, iv: ByteArray?): ByteArray {
        TODO("Not yet implemented")
    }

    override fun decrypt(keyId: KeyId, algorithm: String, plainText: ByteArray, authData: ByteArray?, iv: ByteArray?): ByteArray {
        TODO("Not yet implemented")
    }

}

object SunCryptoService : CryptoService {

    private const val RSA_KEY_SIZE = 4096

    private var ks: KeyStore = LetsTrustServices.load<KeyStore>()

    var ecJWK: ECKey? = null

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
        val key = ks.load(keyId.id)
        val sig = when (key.algorithm) {
            KeyAlgorithm.ECDSA_Secp256k1 -> Signature.getInstance("SHA256withECDSA")
            KeyAlgorithm.EdDSA_Ed25519 -> Signature.getInstance("Ed25519")
        }
        sig.initSign(key.keyPair!!.private)
        sig.update(data)
        return sig.sign()
    }

    override fun verfiy(keyId: KeyId, sig: ByteArray, data: ByteArray): Boolean {
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
}
