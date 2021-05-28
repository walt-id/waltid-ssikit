package org.letstrust.services.key

//import org.bouncycastle.jce.ECNamedCurveTable
//import org.bouncycastle.jce.provider.BouncyCastleProvider
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.*
import com.nimbusds.jose.util.Base64URL
import org.bouncycastle.asn1.ASN1BitString
import org.bouncycastle.asn1.ASN1OctetString
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.jce.ECNamedCurveTable
import org.letstrust.LetsTrustServices
import org.letstrust.crypto.*
import org.letstrust.crypto.keystore.KeyStore
import java.security.interfaces.ECPublicKey
import java.util.*

enum class KeyFormat {
    JWK,
    PEM
}

object KeyService {

    private const val RSA_KEY_SIZE = 4096

    private var cs: CryptoService = LetsTrustServices.load<CryptoService>()

    private var ks: KeyStore = LetsTrustServices.load<KeyStore>()

    fun generate(keyAlgorithm: KeyAlgorithm) = cs.generateKey(keyAlgorithm)

    fun addAlias(keyId: KeyId, alias: String) = ks.addAlias(keyId, alias)

    fun load(keyAlias: String, loadPrivate: Boolean = false) = ks.load(keyAlias, loadPrivate)

    fun export(keyAlias: String, format: KeyFormat = KeyFormat.JWK, exportPrivate: Boolean = false): String =
        when (format) {
            KeyFormat.JWK -> toJwk(keyAlias, exportPrivate).toJSONString()
            else -> toPem(keyAlias, exportPrivate)
        }

    fun toJwk(keyAlias: String, loadPrivate: Boolean = false): JWK {
        return ks.load(keyAlias, loadPrivate).let {
            when (it.algorithm) {
                KeyAlgorithm.EdDSA_Ed25519 -> toEd25519Jwk(it)
                KeyAlgorithm.ECDSA_Secp256k1 -> toSecp256Jwk(it)
                else -> throw IllegalArgumentException("Algorithm not supported")
            }
        }

        throw IllegalArgumentException("No key by alias $keyAlias")
    }

    fun toPem(keyAlias: String, loadPrivate: Boolean = false): String =
        ks.load(keyAlias, loadPrivate).keyPair!!.run {
            (if (loadPrivate) private else public).toPEM()
        }

        throw IllegalArgumentException("No key by alias $keyAlias")
    }

    fun toSecp256Jwk(key: Key): ECKey {
        val builder = ECKey.Builder(Curve.SECP256K1, key.keyPair!!.public as ECPublicKey)
            .keyUse(KeyUse.SIGNATURE)
            .algorithm(JWSAlgorithm.ES256K)
            .keyID(key.keyId.id)

        key.keyPair!!.private?.let {
            builder.privateKey(key.keyPair!!.private)
        }

        return builder.build()
    }

    fun toEd25519Jwk(key: Key): OctetKeyPair {
        val keyUse = KeyUse.parse("sig")
        val keyAlg = JWSAlgorithm.parse("EdDSA")
        val keyCurve = Curve.parse("Ed25519")
        val pubPrim = ASN1Sequence.fromByteArray(key.getPublicKey().encoded) as ASN1Sequence
        val x = (pubPrim.getObjectAt(1) as ASN1BitString).octets

        val builder = OctetKeyPair.Builder(keyCurve, Base64URL.encode(x))
            .keyUse(keyUse)
            .algorithm(keyAlg)
            .keyID(key.keyId.id)

        key.keyPair!!.private?.let {
            val privPrim = ASN1Sequence.fromByteArray(key.keyPair!!.private.encoded) as ASN1Sequence
            var d = (privPrim.getObjectAt(2) as ASN1OctetString).octets
            builder.d(Base64URL.encode(d))
        }

        return builder.build()
    }

    fun listKeys(): List<Key> = ks.listKeys()

    fun delete(alias: String) = ks.delete(alias)

    internal fun setKeyStore(ks: KeyStore) {
        KeyService.ks = ks
    }


    // TODO: consider deprecated methods below

    @Deprecated(message = "outdated")
    private fun generateKeyId(): String = "LetsTrust-Key-${UUID.randomUUID().toString().replace("-", "")}"


    @Deprecated(message = "outdated")
    fun getSupportedCurveNames(): List<String> {
        val ecNames = ArrayList<String>()
        for (name in ECNamedCurveTable.getNames()) {
            ecNames.add(name.toString())
        }
        return ecNames
    }

//    @Deprecated(message = "outdated")
//    fun generateEcKeyPair(ecCurveName: String): String {
//        val generator = KeyPairGenerator.getInstance("ECDSA", "BC")
//        generator.initialize(ECNamedCurveTable.getParameterSpec(ecCurveName), SecureRandom())
//        val keys = Keys(generateKeyId(), generator.generateKeyPair(), "BC")
//        ks.saveKeyPair(keys)
//        return keys.keyId
//    }

//    @Deprecated(message = "outdated")
//    fun generateKeyPair(algorithm: String): String {
//        val keys = when (algorithm) {
//            "Ed25519" -> {
//                HybridConfig.register()
//                val keyPair = Ed25519Sign.KeyPair.newKeyPair()
//                val publicKey = BytePublicKey(keyPair.publicKey, "Ed25519")
//                val privateKey = BytePrivateKey(keyPair.privateKey, "Ed25519")
//                Keys(generateKeyId(), KeyPair(publicKey, privateKey), "Tink")
//            }
//            "Secp256k1" -> {
//                val key = ECKey(SecureRandom())
//                val publicKey = BytePublicKey(key.pubKey, "Secp256k1")
//                val privateKey = BytePrivateKey(key.privKeyBytes, "Secp256k1")
//                Keys(generateKeyId(), KeyPair(publicKey, privateKey), "bitcoinj")
//            }
//            else -> {
//                val generator = KeyPairGenerator.getInstance("RSA", "BC")
//                generator.initialize(RSA_KEY_SIZE)
//                Keys(generateKeyId(), generator.generateKeyPair(), "BC")
//            }
//        }
//        ks.saveKeyPair(keys)
//        return keys.keyId
//    }
//
//    @Deprecated(message = "outdated")
//    fun generateEd25519KeyPair(): String {
//        HybridConfig.register()
//
//        val keyPair = Ed25519Sign.KeyPair.newKeyPair()
//        val publicKey = BytePublicKey(keyPair.publicKey, "Ed25519")
//        val privateKey = BytePrivateKey(keyPair.privateKey, "Ed25519")
//        val keys = Keys(generateKeyId(), KeyPair(publicKey, privateKey), "Tink")
//        ks.saveKeyPair(keys)
//        return keys.keyId
//    }
//
//    @Deprecated(message = "outdated")
//    fun generateEd25519KeyPairNimbus(): String {
//
//        val keyUse = KeyUse.parse("sig")
//        val keyAlg = JWSAlgorithm.parse("EdDSA")
//        val keyCurve = Curve.parse("Ed25519")
//
//        val kp = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
//
//        val keys = Keys(generateKeyId(), kp, "SunEC")
//        ks.saveKeyPair(keys)
//
//        val jwk = KeyUtil.make(kp, keyCurve, keyUse, keyAlg, keys.keyId)
//        if (jwk != null) {
//            println("generateEd25519KeyPairNimbus: " + jwk.toJSONString())
//        }
//
//        return keys.keyId
//
//    }
//
//    @Deprecated(message = "outdated")
//    fun generateSecp256k1KeyPairBitcoinj(): String {
//        val key = ECKey(SecureRandom())
//        val publicKey = BytePublicKey(key.pubKey, "Secp256k1")
//        val privateKey = BytePrivateKey(key.privKeyBytes, "Secp256k1")
//        val keys = Keys(generateKeyId(), KeyPair(publicKey, privateKey), "bitcoinj")
//        ks.saveKeyPair(keys)
//        return keys.keyId
//    }
//
//    @Deprecated(message = "outdated")
//    fun generateSecp256k1KeyPairSun(): String {
//        val keyUse = KeyUse.parse("sig");
//        val keyAlg = JWSAlgorithm.parse("ES256K")
//        val keyCurve = Curve.parse("secp256k1")
//        val ecSpec: ECParameterSpec = keyCurve.toECParameterSpec();
//
//        val generator = KeyPairGenerator.getInstance("EC")
//        generator.initialize(ecSpec)
//
//        val kp = generator.generateKeyPair()
//
//        val pub = kp.getPublic() as ECPublicKey
//        val priv = kp.getPrivate() as ECPrivateKey
//
//        println(priv.format)
//        val keys = Keys(generateKeyId(), kp, "SunEC")
//        ks.saveKeyPair(keys)
//
//        val ecKey = com.nimbusds.jose.jwk.ECKey.Builder(keyCurve, pub)
//            .privateKey(priv)
//            .keyID(keys.keyId)
//            .algorithm(keyAlg)
//            .keyUse(keyUse)
//            .build()
//
//        println(ecKey.toJSONString())
//
//        return keys.keyId
//    }
//
//    @Deprecated(message = "outdated")
//    fun generateRsaKeyPair(): String {
//        val generator = KeyPairGenerator.getInstance("RSA", "BC")
//        generator.initialize(RSA_KEY_SIZE)
//        val keys = Keys(generateKeyId(), generator.generateKeyPair(), "BC")
//        ks.saveKeyPair(keys)
//        return keys.keyId
//    }

//    @Deprecated(message = "outdated")
//    fun loadKeys(keyId: String): Keys? {
//        return ks.getKeyId(keyId)?.let { it -> ks.loadKeyPair(it) }
//    }
//
//    @Deprecated(message = "outdated")
//    fun getMultiBase58PublicKey(keyId: String): String {
//        return ks.loadKeyPair(keyId).let {
//            Multibase.encode(Multibase.Base.Base58BTC, it!!.getPubKey())
//        }
//    }
//
//    @Deprecated(message = "outdated")
//    fun getBase58PublicKey(keyId: String): String? {
//        return ks.loadKeyPair(keyId)?.getPubKey()?.encodeBase58()
//    }


}
