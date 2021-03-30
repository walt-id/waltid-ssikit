package org.letstrust.crypto

import java.security.MessageDigest
import java.security.MessageDigestSpi

open class LtMessageDigestSpi(val algorithm: String) : MessageDigestSpi() {

    val md = MessageDigest.getInstance(algorithm)

    class SHA256 : LtMessageDigestSpi("SHA-256") {}

    override fun engineGetDigestLength(): Int {
        return md.digestLength
    }

    override fun engineUpdate(p0: Byte) {
        md.update(p0)
    }

    override fun engineUpdate(p0: ByteArray?, p1: Int, p2: Int) {
        md.update(p0, p1, p2)
    }

    override fun engineDigest(): ByteArray {
        return md.digest()
    }

    override fun engineReset() {
        md.reset()
    }
}
