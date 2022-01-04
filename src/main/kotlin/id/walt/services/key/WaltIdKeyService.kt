package id.walt.services.key

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.*
import com.nimbusds.jose.util.Base64URL
import id.walt.crypto.*
import id.walt.services.CryptoProvider
import id.walt.services.context.ContextManager
import id.walt.services.crypto.CryptoService
import id.walt.services.keystore.KeyStoreService
import id.walt.services.keystore.KeyType
import mu.KotlinLogging
import org.bouncycastle.asn1.ASN1BitString
import org.bouncycastle.asn1.ASN1OctetString
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.jcajce.provider.digest.Keccak
import org.bouncycastle.util.encoders.Hex
import org.web3j.crypto.ECDSASignature
import org.web3j.crypto.Hash
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

private val log = KotlinLogging.logger {}

open class WaltIdKeyService : KeyService() {

    open val cryptoService: CryptoService = CryptoService.getService()

    open val keyStore: KeyStoreService
        get() = ContextManager.keyStore

    override fun generate(keyAlgorithm: KeyAlgorithm) = cryptoService.generateKey(keyAlgorithm)

    override fun addAlias(keyId: KeyId, alias: String) = keyStore.addAlias(keyId, alias)

    override fun load(keyAlias: String): Key = keyStore.load(keyAlias, KeyType.PUBLIC)
    override fun load(keyAlias: String, keyType: KeyType) = keyStore.load(keyAlias, keyType)

    override fun export(keyAlias: String, format: KeyFormat, exportKeyType: KeyType): String =
        when (format) {
            KeyFormat.JWK -> toJwk(keyAlias, exportKeyType).toJSONString()
            else -> toPem(keyAlias, exportKeyType)
        }

    override fun importKey(keyStr: String): KeyId {
        val key = parseJwkKey(keyStr)
        log.debug { "Importing key ${key.keyId}" }
        keyStore.store(key)
        return key.keyId
    }

    private fun parseJwkKey(jwkKeyStr: String): Key {
        val jwk = JWK.parse(jwkKeyStr)

        val key = when (jwk.algorithm?.name) {
            "EdDSA" -> buildKey(
                keyId = jwk.keyID,
                algorithm = KeyAlgorithm.EdDSA_Ed25519.name,
                provider = CryptoProvider.SUN.name,
                publicPart = jwk.toOctetKeyPair().x.toString(),
                privatePart = jwk.toOctetKeyPair().d?.let { jwk.toOctetKeyPair().d.toString() },
                format = id.walt.crypto.KeyFormat.BASE64_RAW
            )
            "ES256K" -> Key(
                keyId = KeyId(jwk.keyID),
                algorithm = KeyAlgorithm.ECDSA_Secp256k1,
                cryptoProvider = CryptoProvider.SUN,
                keyPair = jwk.toECKey().toKeyPair()
            )
            "RSA" -> Key(
                keyId = KeyId(jwk.keyID),
                algorithm = KeyAlgorithm.RSA,
                cryptoProvider = CryptoProvider.SUN,
                keyPair = jwk.toRSAKey().toKeyPair()
            )
            null -> {
                when (jwk.keyType?.value) {
                    "RSA" -> Key(
                        keyId = KeyId(jwk.keyID ?: newKeyId().id),
                        algorithm = KeyAlgorithm.RSA,
                        cryptoProvider = CryptoProvider.SUN,
                        keyPair = jwk.toRSAKey().toKeyPair()
                    )
                    else -> throw IllegalArgumentException("KeyType ${jwk.keyType} / Algorithm ${jwk.algorithm} not supported")
                }
            }
            else -> {
                throw IllegalArgumentException("Algorithm ${jwk.algorithm} / KeyType ${jwk.keyType} not supported")
            }
        }
        return key
    }

    override fun toJwk(keyAlias: String, keyType: KeyType, jwkKeyId: String?): JWK {
        return keyStore.load(keyAlias, keyType).let {
            when (it.algorithm) {
                KeyAlgorithm.EdDSA_Ed25519 -> toEd25519Jwk(it, jwkKeyId)
                KeyAlgorithm.ECDSA_Secp256k1 -> toSecp256Jwk(it, jwkKeyId)
                KeyAlgorithm.RSA -> toRsaJwk(it, jwkKeyId)
                else -> throw IllegalArgumentException("Algorithm not supported")
            }
        }
    }

    override fun toPem(keyAlias: String, keyType: KeyType): String =
        keyStore.load(keyAlias, keyType).keyPair!!.run {
            (if (keyType == KeyType.PRIVATE) private else public).toPEM()
        }

    override fun toSecp256Jwk(key: Key, jwkKeyId: String?): ECKey {
        val builder = ECKey.Builder(Curve.SECP256K1, key.keyPair!!.public as ECPublicKey)
            .keyUse(KeyUse.SIGNATURE)
            .algorithm(JWSAlgorithm.ES256K)
            .keyID(jwkKeyId ?: key.keyId.id)

        key.keyPair!!.private?.let {
            builder.privateKey(key.keyPair!!.private)
        }

        return builder.build()
    }

    override fun toRsaJwk(key: Key, jwkKeyId: String?): RSAKey {

        val builder = RSAKey.Builder(key.keyPair!!.public as RSAPublicKey)
            .privateKey(key.keyPair!!.private as RSAPrivateKey) // TODO is needed?
            .keyUse(KeyUse.SIGNATURE)
            .algorithm(JWSAlgorithm.RS256)
            .keyID(jwkKeyId ?: key.keyId.id)

        key.keyPair!!.private?.let {
            builder.privateKey(key.keyPair!!.private)
        }

        return builder.build()
    }

    override fun toEd25519Jwk(key: Key, jwkKeyId: String?): OctetKeyPair {
        val keyUse = KeyUse.parse("sig")
        val keyAlg = JWSAlgorithm.parse("EdDSA")
        val keyCurve = Curve.parse("Ed25519")
        val pubPrim = ASN1Sequence.fromByteArray(key.getPublicKey().encoded) as ASN1Sequence
        val x = (pubPrim.getObjectAt(1) as ASN1BitString).octets

        val builder = OctetKeyPair.Builder(keyCurve, Base64URL.encode(x))
            .keyUse(keyUse)
            .algorithm(keyAlg)
            .keyID(jwkKeyId ?: key.keyId.id)

        key.keyPair!!.private?.let {
            val privPrim = ASN1Sequence.fromByteArray(key.keyPair!!.private.encoded) as ASN1Sequence
            var d = (privPrim.getObjectAt(2) as ASN1OctetString).octets

            if (d.size > 32 && d[0].toInt() == 0x04 && d[1].toInt() == 0x20) {
                d = (ASN1OctetString.fromByteArray(d) as ASN1OctetString).octets
            }

            builder.d(Base64URL.encode(d))
        }

        return builder.build()
    }

    override fun getEthereumAddress(keyAlias: String): String =
        keyStore.load(keyAlias).let {
            when (it.algorithm) {
                KeyAlgorithm.ECDSA_Secp256k1 -> calculateEthereumAddress(toSecp256Jwk(it))
                else -> throw IllegalArgumentException("Algorithm not supported")
            }
        }

    private fun calculateEthereumAddress(key: ECKey): String {
        val digest = Keccak.Digest256().digest(key.x.decode().copyOfRange(0, 32) + key.y.decode().copyOfRange(0, 32))
        return String(Hex.encode(digest)).let { sha3_256hex ->
            Keys.toChecksumAddress(sha3_256hex.substring(sha3_256hex.length - 40)) //.toLowerCase()
        }
    }

    override fun getRecoveryId(keyAlias: String, data: ByteArray, sig: ECDSASignature): Int {
        for (i in 0..3) {
            Sign.recoverFromSignature(i, sig, Hash.sha3(data))?.let {
                val address = Numeric.prependHexPrefix(getEthereumAddress(keyAlias))
                val recoveredAddress = Keys.toChecksumAddress(Numeric.prependHexPrefix(Keys.getAddress(it)))
                if (address == recoveredAddress) return i
            }
        }
        throw IllegalStateException("Could not construct a recoverable key. This should never happen.")
    }

    override fun listKeys(): List<Key> = keyStore.listKeys()

    override fun delete(alias: String) = keyStore.delete(alias)

    override fun hasKey(alias: String): Boolean = keyStore.getKeyId(alias) != null
}


//    @Deprecated(message = "outdated")
//    override fun getSupportedCurveNames(): List<String> {
//        val ecNames = ArrayList<String>()
//        for (name in ECNamedCurveTable.getNames()) {
//            ecNames.add(name.toString())
//        }
//        return ecNames
//    }

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
