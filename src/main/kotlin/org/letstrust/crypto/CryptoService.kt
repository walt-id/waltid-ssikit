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
import org.letstrust.*
import org.letstrust.services.key.KeyManagementService
import org.letstrust.services.key.KeyStore
import org.letstrust.services.key.TinkKeyStore
import java.security.InvalidKeyException
import java.security.KeyPair
import java.security.KeyPairGenerator


interface CryptoService {

    fun generateKey(algorithm: KeyAlgorithm): KeyId
    fun sign(keyId: KeyId, data: ByteArray): ByteArray
    fun verfiy(keyId: KeyId, sig: ByteArray, data: ByteArray): Boolean
}

data class Key(val keyId: KeyId, val keyAlgorithm: KeyAlgorithm, val cryptoProvider: CryptoProvider) {
    constructor(keyId: KeyId, keyAlgorithm: KeyAlgorithm, cryptoProvider: CryptoProvider, keyPair: KeyPair) : this(keyId, keyAlgorithm, cryptoProvider) {
        this.keyPair = keyPair
    }

    constructor(keyId: KeyId, keyAlgorithm: KeyAlgorithm, cryptoProvider: CryptoProvider, keysetHandle: KeysetHandle) : this(keyId, keyAlgorithm, cryptoProvider) {
        this.keysetHandle = keysetHandle
    }

    var keyPair: KeyPair? = null
    var keysetHandle: KeysetHandle? = null
}


object TinkCryptoService : CryptoService {
    private var ks: KeyStore = TinkKeyStore //LetsTrustServices.load<KeyStore>()
    override fun generateKey(algorithm: KeyAlgorithm): KeyId {
        val keyId = generateKeyId()

        val keysetHandle = when (algorithm) {
            KeyAlgorithm.Secp256k1 -> KeysetHandle.generateNew(EcdsaSignKeyManager.ecdsaP256Template())
            KeyAlgorithm.Ed25519 -> KeysetHandle.generateNew(Ed25519PrivateKeyManager.ed25519Template())
        }

        println(keysetHandle)

        val key = Key(keyId, KeyAlgorithm.Secp256k1, CryptoProvider.TINK, keysetHandle)
        ks.store(key)
        return keyId
    }

    override fun sign(keyId: KeyId, data: ByteArray): ByteArray {
        val key = ks.load(keyId)
        val signer: PublicKeySign = key.keysetHandle!!.getPrimitive(PublicKeySign::class.java)
        return signer.sign(data)
    }

    override fun verfiy(keyId: KeyId, sig: ByteArray, data: ByteArray): Boolean {
        val key = ks.load(keyId)
        val verifier: PublicKeyVerify = key.keysetHandle!!.getPrimitive(PublicKeyVerify::class.java)
        return verifier.verify(sig, data);
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
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(RSA_KEY_SIZE)
        val keyPair = generator.genKeyPair()
        val key = Key(generateKeyId(), KeyAlgorithm.Secp256k1, CryptoProvider.SUN, keyPair)
        ks.store(key)
        return key.keyId
    }

    override fun sign(keyId: KeyId, data: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

    override fun verfiy(keyId: KeyId, sig: ByteArray): Boolean {
        TODO("Not yet implemented")
    }
}
