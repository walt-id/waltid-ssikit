package id.walt.services.key

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.OctetKeyPair
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey

data class BytePrivateKey(val privateKey: ByteArray, private val algorithm: String) : PrivateKey {
    override fun getAlgorithm(): String {
        return this.algorithm
    }

    override fun getFormat(): String {
        return "byte"
    }

    override fun getEncoded(): ByteArray {
        return this.privateKey
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BytePrivateKey

        if (!privateKey.contentEquals(other.privateKey)) return false
        if (algorithm != other.algorithm) return false

        return true
    }

    override fun hashCode(): Int {
        var result = privateKey.contentHashCode()
        result = 31 * result + algorithm.hashCode()
        return result
    }
}

data class BytePublicKey(val publicKey: ByteArray, private val algorithm: String) : PublicKey {
    override fun getAlgorithm(): String {
        return this.algorithm
    }

    override fun getFormat(): String {
        return "byte"
    }

    override fun getEncoded(): ByteArray {
        return this.publicKey
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BytePublicKey

        if (!publicKey.contentEquals(other.publicKey)) return false
        if (algorithm != other.algorithm) return false

        return true
    }

    override fun hashCode(): Int {
        var result = publicKey.contentHashCode()
        result = 31 * result + algorithm.hashCode()
        return result
    }
}


data class Keys(val keyId: String, val pair: KeyPair, val provider: String) {

    var algorithm: String = pair.private.algorithm

    // A hack, required for ld-signatures
    fun getPrivateAndPublicKey(): ByteArray {

        val privAndPubKey = ByteArray(64)
        System.arraycopy(getPrivKey(), 0, privAndPubKey, 0, 32)
        System.arraycopy(getPubKey(), 0, privAndPubKey, 32, 32)

        return privAndPubKey
    }

    fun isByteKey(): Boolean = this.pair.private is BytePrivateKey

    fun getPubKey(): ByteArray {
        if (provider == "SunEC") {
            return toOctetKeyPair().decodedX
        }
        return (this.pair.public as BytePublicKey).publicKey
    }

    fun getPrivKey(): ByteArray {
        if (provider == "SunEC") {
            return toOctetKeyPair().decodedD
        }
        return (this.pair.private as BytePrivateKey).privateKey
    }

    fun toOctetKeyPair(): OctetKeyPair {
        val keyUse = KeyUse.parse("sig")
        val keyAlg = JWSAlgorithm.parse("EdDSA")
        val keyCurve = Curve.parse("Ed25519")
        return KeyUtil.make(this.pair, keyCurve, keyUse, keyAlg, keyId)
    }

    fun toEcKey(): ECKey {
        val keyUse = KeyUse.parse("sig")
        val keyAlg = JWSAlgorithm.parse("ES256K")
        val keyCurve = Curve.parse("secp256k1")

        val pub = pair.public as ECPublicKey
        val priv = pair.private as ECPrivateKey

        return ECKey.Builder(keyCurve, pub)
            .privateKey(priv)
            .keyID(keyId)
            .algorithm(keyAlg)
            .keyUse(keyUse)
            .build()
    }

    fun exportJwk(): String {
        if (provider == "SunEC") {
            return when (algorithm) {
                "ed" -> toOctetKeyPair().toJSONString()
                "sec" -> toEcKey().toJSONString()
                else -> throw UnsupportedOperationException("Export of algo: $algorithm not supported")
            }
        }
        throw UnsupportedOperationException("Export of for provider: $provider not supported")
    }
}

