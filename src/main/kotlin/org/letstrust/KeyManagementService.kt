package org.letstrust

import com.google.crypto.tink.config.TinkConfig
import com.google.crypto.tink.subtle.Ed25519Sign
import io.ipfs.multibase.Multibase
import org.bitcoinj.core.ECKey
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
//import org.bouncycastle.jce.ECNamedCurveTable
//import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Security
import java.util.*
import kotlin.collections.ArrayList


object KeyManagementService {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    // TODO: keystore implementation should be configurable
    private var ks = SqlKeyStore as KeyStore

    private fun generateKeyId(): String = "LetsTrust-Key-${UUID.randomUUID().toString().replace("-", "")}"

    fun setKeyStore(ks: KeyStore) {
        KeyManagementService.ks = ks
    }

    fun getSupportedCurveNames(): List<String> {
        var ecNames = ArrayList<String>()
        for (name in ECNamedCurveTable.getNames()) {
            ecNames.add(name.toString())
        }
        return ecNames
    }

    fun generateEcKeyPair(ecCurveName: String): String {
        val generator = KeyPairGenerator.getInstance("ECDSA", "BC")
        generator.initialize(ECNamedCurveTable.getParameterSpec(ecCurveName), SecureRandom())

        val keys = Keys(generateKeyId(), generator.generateKeyPair(), "BC")
        ks.saveKeyPair(keys)
        return keys.keyId
    }

    fun generateKeyPair(algorithm: String): String {

        var keys = when (algorithm) {
            "Ed25519" -> {
                TinkConfig.register()
                var keyPair = Ed25519Sign.KeyPair.newKeyPair()
                var publicKey = BytePublicKey(keyPair.publicKey, "Ed25519")
                var privateKey = BytePrivateKey(keyPair.privateKey, "Ed25519")
                Keys(generateKeyId(), KeyPair(publicKey, privateKey), "Tink")
            }
            "Secp256k1" -> {
                var key = ECKey(SecureRandom())
                var publicKey = BytePublicKey(key.pubKey, "Secp256k1")
                var privateKey = BytePrivateKey(key.privKeyBytes, "Secp256k1")
                Keys(generateKeyId(), KeyPair(publicKey, privateKey), "bitcoinj")
            }
            else -> {
                val generator = KeyPairGenerator.getInstance("RSA", "BC")
                generator.initialize(1024)
                Keys(generateKeyId(), generator.generateKeyPair(), "BC")
            }
        }
        ks.saveKeyPair(keys)
        return keys.keyId
    }


    fun generateEd25519KeyPair(): String {
        TinkConfig.register()

        var keyPair = Ed25519Sign.KeyPair.newKeyPair()
        var publicKey = org.letstrust.BytePublicKey(keyPair.publicKey, "Ed25519")
        var privateKey = org.letstrust.BytePrivateKey(keyPair.privateKey, "Ed25519")
        val keys = org.letstrust.Keys(generateKeyId(), KeyPair(publicKey, privateKey), "Tink")
        ks.saveKeyPair(keys)
        return keys.keyId
    }

    fun generateSecp256k1KeyPair(): String {
        var key = ECKey(SecureRandom())
        var publicKey = org.letstrust.BytePublicKey(key.pubKey, "Secp256k1")
        var privateKey = org.letstrust.BytePrivateKey(key.privKeyBytes, "Secp256k1")
        val keys = org.letstrust.Keys(generateKeyId(), KeyPair(publicKey, privateKey), "bitcoinj")
        ks.saveKeyPair(keys)
        return keys.keyId
    }

    fun generateRsaKeyPair(): String {
        val generator = KeyPairGenerator.getInstance("RSA", "BC")
        generator.initialize(1024)
        val keys = org.letstrust.Keys(generateKeyId(), generator.generateKeyPair(), "BC")
        ks.saveKeyPair(keys)
        return keys.keyId
    }

    fun loadKeys(keyId: String): Keys? {
        return ks.getKeyId(keyId)?.let { it -> ks.loadKeyPair(it) }
        return null
    }

    fun listkeys(): List<Keys> {
        return ks.listkeys()
    }

    fun deleteKeys(keyId: String) {
        ks.deleteKeyPair(ks.getKeyId(keyId)!!)
    }

    fun getMultiBase58PublicKey(keyId: String): String {
        return ks.loadKeyPair(keyId).let {
            Multibase.encode(Multibase.Base.Base58BTC, it!!.getPubKey())
        }
    }

    fun getBase58PublicKey(keyId: String): String? {
        return ks.loadKeyPair(keyId)?.getPubKey()?.encodeBase58()
    }

    fun addAlias(keyId: String, identifier: String) {
        ks.addAlias(keyId, identifier)
    }

}
