package org.letstrust.crypto

import java.security.AlgorithmParameters
import java.security.Key
import java.security.SecureRandom
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.CipherSpi

open class LtCipherSpi(val algorithm: String): CipherSpi() {

    val c = Cipher.getInstance(algorithm)

    class AES256_GCM_NoPadding : LtCipherSpi("AES/GCM/NoPadding") {}

    override fun engineSetMode(p0: String?) {
        TODO("Not yet implemented")
    }

    override fun engineSetPadding(p0: String?) {
        TODO("Not yet implemented")
    }

    override fun engineGetBlockSize(): Int {
        return c.blockSize
    }

    override fun engineGetOutputSize(p0: Int): Int {
        TODO("Not yet implemented")
    }

    override fun engineGetIV(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun engineGetParameters(): AlgorithmParameters {
       return c.parameters
    }

    override fun engineInit(p0: Int, p1: Key?, p2: SecureRandom?) {
        TODO("Not yet implemented")
    }

    override fun engineInit(p0: Int, p1: Key?, p2: AlgorithmParameterSpec?, p3: SecureRandom?) {
        c.init(p0, p1, p2, p3)
    }

    override fun engineInit(p0: Int, p1: Key?, p2: AlgorithmParameters?, p3: SecureRandom?) {
        TODO("Not yet implemented")
    }

    override fun engineUpdate(p0: ByteArray?, p1: Int, p2: Int): ByteArray {
        TODO("Not yet implemented")
    }

    override fun engineUpdate(p0: ByteArray?, p1: Int, p2: Int, p3: ByteArray?, p4: Int): Int {
        TODO("Not yet implemented")
    }

    override fun engineDoFinal(p0: ByteArray?, p1: Int, p2: Int): ByteArray {
        return c.doFinal(p0, p1, p2)
    }

    override fun engineDoFinal(p0: ByteArray?, p1: Int, p2: Int, p3: ByteArray?, p4: Int): Int {
        TODO("Not yet implemented")
    }

    override fun engineUpdateAAD(src: ByteArray?, offset: Int, len: Int) {
        c.updateAAD(src, offset, len)
    }
}
