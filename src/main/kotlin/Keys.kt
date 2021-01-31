import com.nimbusds.jose.util.Base64
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey

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
}

data class Keys(val keyId: String, val pair: KeyPair, val provider: String) {

    var algorithm: String = pair.private.algorithm

    // A hack to get ld-signatures to work
    fun getPrivateAndPublicKey(): ByteArray? {

        val privAndPubKey = ByteArray(64)
        System.arraycopy((this.pair!!.private as BytePrivateKey).privateKey, 0, privAndPubKey, 0, 32)
        System.arraycopy((this.pair!!.public as BytePublicKey).publicKey, 0, privAndPubKey, 32, 32)

        return privAndPubKey
    }

    fun isByteKey(): Boolean {
        return this.pair.private is BytePrivateKey
    }
}
