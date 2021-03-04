package org.letstrust

//import org.bouncycastle.jce.ECNamedCurveTable
//import org.bouncycastle.jce.provider.BouncyCastleProvider
import com.google.crypto.tink.hybrid.HybridConfig
import com.google.crypto.tink.subtle.Ed25519Sign
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.Curve.SECP256K1
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator
import io.ipfs.multibase.Multibase
import org.bitcoinj.core.ECKey
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.Security
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECParameterSpec
import java.util.*
import kotlin.collections.ArrayList

object KeyManagementService {

    private const val RSA_KEY_SIZE = 4096

    // TODO: keystore implementation should be configurable
    private var ks: KeyStore = FileSystemKeyStore as KeyStore
    // private var ks = FileSystemKeyStore as KeyStore

    init {
        Security.addProvider(BouncyCastleProvider())
        ks = SqlKeyStore as KeyStore
    }

    private fun generateKeyId(): String = "LetsTrust-Key-${UUID.randomUUID().toString().replace("-", "")}"

    fun setKeyStore(ks: KeyStore) {
        KeyManagementService.ks = ks
    }

    fun getSupportedCurveNames(): List<String> {
        val ecNames = ArrayList<String>()
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
        val keys = when (algorithm) {
            "Ed25519" -> {
                HybridConfig.register()
                val keyPair = Ed25519Sign.KeyPair.newKeyPair()
                val publicKey = BytePublicKey(keyPair.publicKey, "Ed25519")
                val privateKey = BytePrivateKey(keyPair.privateKey, "Ed25519")
                Keys(generateKeyId(), KeyPair(publicKey, privateKey), "Tink")
            }
            "Secp256k1" -> {
                val key = ECKey(SecureRandom())
                val publicKey = BytePublicKey(key.pubKey, "Secp256k1")
                val privateKey = BytePrivateKey(key.privKeyBytes, "Secp256k1")
                Keys(generateKeyId(), KeyPair(publicKey, privateKey), "bitcoinj")
            }
            else -> {
                val generator = KeyPairGenerator.getInstance("RSA", "BC")
                generator.initialize(RSA_KEY_SIZE)
                Keys(generateKeyId(), generator.generateKeyPair(), "BC")
            }
        }
        ks.saveKeyPair(keys)
        return keys.keyId
    }

    fun generateEd25519KeyPair(): String {
        HybridConfig.register()

        val keyPair = Ed25519Sign.KeyPair.newKeyPair()
        val publicKey = BytePublicKey(keyPair.publicKey, "Ed25519")
        val privateKey = BytePrivateKey(keyPair.privateKey, "Ed25519")
        val keys = Keys(generateKeyId(), KeyPair(publicKey, privateKey), "Tink")
        ks.saveKeyPair(keys)
        return keys.keyId
    }

    fun generateEd25519KeyPairNimbus(): String {

        val keyUse = KeyUse.parse("sig")
        val keyAlg = JWSAlgorithm.parse("EdDSA")
        val keyCurve = Curve.parse("Ed25519")

        val kp = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()

        println("default format: " + kp.private.format)
//        return keys.keyId

        val keys = Keys(generateKeyId(), kp, "sun")
        ks.saveKeyPair(keys)

        val jwk = KeyUtil.make(keyCurve, keyUse, keyAlg, keys.keyId)
        if (jwk != null) {
            println("JWK format: " + jwk.toJSONString())
        }

        return keys.keyId

    }

    fun generateSecp256k1KeyPairBitcoinj(): String {
        val key = ECKey(SecureRandom())
        val publicKey = BytePublicKey(key.pubKey, "Secp256k1")
        val privateKey = BytePrivateKey(key.privKeyBytes, "Secp256k1")
        val keys = Keys(generateKeyId(), KeyPair(publicKey, privateKey), "bitcoinj")
        ks.saveKeyPair(keys)
        return keys.keyId
    }

    fun generateSecp256k1KeyPairSun(): String {
        val keyUse = KeyUse.parse("sig");
        val keyAlg = JWSAlgorithm.parse("ES256K")
        val keyCurve = Curve.parse("secp256k1")
        val ecSpec: ECParameterSpec = keyCurve.toECParameterSpec();

        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ecSpec)

        val kp = generator.generateKeyPair()

        val pub = kp.getPublic() as ECPublicKey
        val priv = kp.getPrivate() as ECPrivateKey

        println(priv.format)
        val keys = Keys(generateKeyId(), kp, "sun")
        ks.saveKeyPair(keys)

        val ecKey = com.nimbusds.jose.jwk.ECKey.Builder(keyCurve, pub)
            .privateKey(priv)
            .keyID(keys.keyId)
            .algorithm(keyAlg)
            .keyUse(keyUse)
            .build()

        println(ecKey.toJSONString())

        return keys.keyId
    }

    fun generateRsaKeyPair(): String {
        val generator = KeyPairGenerator.getInstance("RSA", "BC")
        generator.initialize(RSA_KEY_SIZE)
        val keys = Keys(generateKeyId(), generator.generateKeyPair(), "BC")
        ks.saveKeyPair(keys)
        return keys.keyId
    }

    fun loadKeys(keyId: String): Keys? {
        return ks.getKeyId(keyId)?.let { it -> ks.loadKeyPair(it) }
    }

    fun listKeys(): List<Keys> {
        return ks.listKeys()
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
