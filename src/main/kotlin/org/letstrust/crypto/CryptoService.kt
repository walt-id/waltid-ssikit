package org.letstrust.crypto

import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.PublicKeySign
import com.google.crypto.tink.PublicKeyVerify
import com.google.crypto.tink.signature.EcdsaSignKeyManager
import com.google.crypto.tink.signature.Ed25519PrivateKeyManager
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.impl.ECDSA
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import org.bouncycastle.jce.ECNamedCurveTable
import org.letstrust.CryptoProvider
import org.letstrust.KeyAlgorithm
import org.letstrust.LetsTrustServices
import org.letstrust.newKeyId
import org.letstrust.services.key.KeyManagementService
import org.letstrust.services.key.KeyStore
import org.letstrust.services.key.TinkKeyStore
import java.security.InvalidKeyException
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Signature


interface CryptoService {

    fun generateKey(algorithm: KeyAlgorithm): KeyId
    fun sign(keyId: KeyId, data: ByteArray): ByteArray
    fun verfiy(keyId: KeyId, sig: ByteArray, data: ByteArray): Boolean
}


object TinkCryptoService : CryptoService {
    private var ks: KeyStore = TinkKeyStore //LetsTrustServices.load<KeyStore>()
    override fun generateKey(algorithm: KeyAlgorithm): KeyId {

        val keysetHandle = when (algorithm) {
            KeyAlgorithm.Secp256k1 -> KeysetHandle.generateNew(EcdsaSignKeyManager.ecdsaP256Template())
            KeyAlgorithm.Ed25519 -> KeysetHandle.generateNew(Ed25519PrivateKeyManager.ed25519Template())
        }

        println(keysetHandle)

        val key = Key(newKeyId(), algorithm, CryptoProvider.TINK, keysetHandle)
        ks.store(key)
        return key.keyId
    }

    override fun sign(keyId: KeyId, data: ByteArray): ByteArray {
        val key = ks.load(keyId)
        val signer: PublicKeySign = key.keysetHandle!!.getPrimitive(PublicKeySign::class.java)
        return signer.sign(data)
    }

    override fun verfiy(keyId: KeyId, sig: ByteArray, data: ByteArray): Boolean {
        val key = ks.load(keyId)!!
        val verifier: PublicKeyVerify = key.keysetHandle!!.publicKeysetHandle.getPrimitive(PublicKeyVerify::class.java)
        try {
            verifier.verify(sig, data);
        } catch (e: Exception) {
            return false
        }
        return true
    }

}

object SunCryptoService : CryptoService {

    private const val RSA_KEY_SIZE = 4096

    private var ks: KeyStore = LetsTrustServices.load<KeyStore>()

    var ecJWK: ECKey? = null

    fun generate(): String {
        ecJWK = ECKeyGenerator(Curve.SECP256K1)
            .keyUse(KeyUse.SIGNATURE)
            .keyID("123")
            .generate()
        return ecJWK!!.keyID
    }

    fun sign(keyId: String, data: ByteArray): ByteArray {

        val signingKey = KeyManagementService.loadKeys(keyId)

        val jcaSignature = try {
            val dsa = ECDSA.getSignerAndVerifier(JWSAlgorithm.ES256K, null)
            dsa.initSign(signingKey!!.toEcKey().toECPrivateKey())
            dsa.update(data)
            dsa.sign()
        } catch (e: InvalidKeyException) {
            throw JOSEException(e.message, e)
        }
        return jcaSignature
    }

    fun verify(keyId: String, signature: ByteArray) {

    }

    override fun generateKey(algorithm: KeyAlgorithm): KeyId {

        val generator = when (algorithm) {
            KeyAlgorithm.Secp256k1 -> {
                val generator = KeyPairGenerator.getInstance("ECDSA")
                generator.initialize(ECNamedCurveTable.getParameterSpec("secp256k1"), SecureRandom())
                generator
            }
            KeyAlgorithm.Ed25519 -> {
                val generator = KeyPairGenerator.getInstance("Ed25519")
                generator
            }
        }

//        val generator = KeyPairGenerator.getInstance("RSA")
//        generator.initialize(RSA_KEY_SIZE)

        val keyPair = generator.generateKeyPair()
        val key = Key(newKeyId(), KeyAlgorithm.Secp256k1, CryptoProvider.SUN, keyPair)
        ks.store(key)
        return key.keyId
    }

    override fun sign(keyId: KeyId, data: ByteArray): ByteArray {
        val key = ks.load(keyId)
        val sig = when (key.algorithm) {
            KeyAlgorithm.Secp256k1 -> Signature.getInstance("SHA256withECDSA")
            KeyAlgorithm.Ed25519 -> Signature.getInstance("Ed25519")
        }
        sig.initSign(key.keyPair!!.private)
        sig.update(data)
        return sig.sign()
    }

    override fun verfiy(keyId: KeyId, sig: ByteArray, data: ByteArray): Boolean {
        val key = ks.load(keyId)
        val signature = when (key.algorithm) {
            KeyAlgorithm.Secp256k1 -> Signature.getInstance("SHA256withECDSA")
            KeyAlgorithm.Ed25519 -> Signature.getInstance("Ed25519")
        }
        signature.initVerify(key.keyPair!!.public)
        signature.update(data)
        return signature.verify(sig)
    }
}
