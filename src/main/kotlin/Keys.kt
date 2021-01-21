import com.google.crypto.tink.subtle.Ed25519Sign
import org.apache.commons.codec.binary.Hex
import java.security.KeyPair

class Keys (val keyId: String){
    var pair: KeyPair? = null
    var privateKey: ByteArray? = null
    var publicKey: ByteArray? = null

    constructor(keyId: String, pair: KeyPair) : this(keyId) {
        this.pair = pair
    }
    constructor(keyId: String, privateKey: ByteArray, publicKey: ByteArray) : this(keyId) {
        this.privateKey = privateKey
        this.publicKey = publicKey

//        println("privateKey: ")
//        println(Hex.encodeHex(privateKey))
//        print("publicKey: ")
//        println(Hex.encodeHex(publicKey))

    }

    // A hack to get ld-signatures to work
    fun getPrivateAndPublicKey(): ByteArray? {

        val privAndPubKey = ByteArray(64)
        System.arraycopy(this.privateKey, 0, privAndPubKey, 0, 32)
        System.arraycopy(this.publicKey, 0, privAndPubKey, 32, 32)

        return privAndPubKey
    }
}
