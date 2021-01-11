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
    }
}
