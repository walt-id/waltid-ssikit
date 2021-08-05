package id.walt.crypto

import java.security.MessageDigest
import java.security.MessageDigestSpi

open class LtMessageDigestSpi(val algorithm: String) : MessageDigestSpi() {

    val md: MessageDigest = MessageDigest.getInstance(algorithm)

    class SHA256 : LtMessageDigestSpi("SHA-256")

    override fun engineGetDigestLength(): Int {
        return md.digestLength
    }

    override fun engineUpdate(input: Byte) {
        md.update(input)
    }

    override fun engineUpdate(input: ByteArray?, offset: Int, len: Int) {
        md.update(input, offset, len)
    }

    override fun engineDigest(): ByteArray {
        return md.digest()
    }

    override fun engineReset() {
        md.reset()
    }
}
