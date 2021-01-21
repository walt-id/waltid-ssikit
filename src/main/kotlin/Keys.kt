import java.security.KeyPair

class Keys(val keyId: String) {
    var pair: KeyPair? = null
    var privateKey: ByteArray? = null
    var publicKey: ByteArray? = null
    var algorithm: String? = null
    var provider: String? = null

    constructor(keyId: String, pair: KeyPair, algorithm: String?, provider: String?) : this(keyId) {
        this.pair = pair
        this.algorithm = algorithm
        this.provider = provider
    }

    constructor(keyId: String, privateKey: ByteArray, publicKey: ByteArray, algorithm: String?, provider: String?) : this(
        keyId
    ) {
        this.privateKey = privateKey
        this.publicKey = publicKey
        this.algorithm = algorithm
        this.provider = provider

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
