package org.letstrust.crypto

import org.letstrust.LetsTrustServices
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SignatureSpi

open class LtSignature(val algorithm: String) : SignatureSpi() {

    class SHA1withECDSA : LtSignature("SHA1withECDSA")
    class SHA224withECDSA : LtSignature("SHA224withECDSA")
    class SHA256withECDSA : LtSignature("SHA256withECDSA")
    class SHA384withECDSA : LtSignature("SHA384withECDSAS")
    class SHA512withECDSA : LtSignature("SHA512withECDSA")

    var keyId: KeyId? = null

    var b: ByteArray? = null
    var off: Int? = null
    var len: Int? = null

    val cryptoService = LetsTrustServices.load<CryptoService>()

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
        return cryptoService.sign(keyId!!, b!!)
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