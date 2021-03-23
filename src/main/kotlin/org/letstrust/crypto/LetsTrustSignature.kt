package org.letstrust.crypto

import java.security.PrivateKey
import java.security.PublicKey
import java.security.SignatureSpi

open class LetsTrustSignature(val algorithm: String) : SignatureSpi() {

    class SHA1 : LetsTrustSignature("SHA1")
    class SHA224 : LetsTrustSignature("SHA-224")
    class SHA256 : LetsTrustSignature("SHA-256")
    class SHA384 : LetsTrustSignature("SHA-384")
    class SHA512 : LetsTrustSignature("SHA-512")

    var keyId: String? = null

    var b: ByteArray? = null
    var off: Int? = null
    var len: Int? = null


    override fun engineInitVerify(publicKey: PublicKey?) {
        TODO("Not yet implemented")
    }

    override fun engineInitSign(privateKey: PrivateKey?) {
        keyId = (privateKey as PrivateKeyHandle).keyId
    }

    override fun engineUpdate(b: Byte) {
        TODO("Not yet implemented")
    }

    override fun engineUpdate(b: ByteArray?, off: Int, len: Int) {
        this.b = b
        this.off = off
        this.len = len
    }

    override fun engineSign(): ByteArray {
        return CryptoService.sign(keyId!!, b!!)
    }

    override fun engineVerify(sigBytes: ByteArray?): Boolean {
        TODO("Not yet implemented")
    }

    override fun engineSetParameter(param: String?, value: Any?) {
        TODO("Not yet implemented")
    }

    override fun engineGetParameter(param: String?): Any {
        TODO("Not yet implemented")
    }

}