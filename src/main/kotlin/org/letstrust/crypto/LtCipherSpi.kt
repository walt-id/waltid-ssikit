package org.letstrust.crypto

import org.letstrust.LetsTrustServices
import java.security.AlgorithmParameters
import java.security.Key
import java.security.SecureRandom
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.CipherSpi
import javax.crypto.spec.GCMParameterSpec

open class LtCipherSpi(val algorithm: String) : CipherSpi() {

    val c = Cipher.getInstance(algorithm)

    val cryptoService = LetsTrustServices.load<CryptoService>()

    class AES256_GCM_NoPadding : LtCipherSpi("AES/GCM/NoPadding") {}

    var isEncryptionMode = true;
    var iv: ByteArray? = null
    var authData: ByteArray? = null
    var plainText: ByteArray? = null

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
        if (p0 == 2) isEncryptionMode = false
        iv = (p2 as GCMParameterSpec).iv
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
        plainText = p0
        if (isEncryptionMode) {
            return cryptoService.encrypt(KeyId("123"), algorithm, plainText!!, authData, iv)
        }
        return cryptoService.decrypt(KeyId("123"), algorithm, plainText!!, authData, iv)

        //    return c.doFinal(p0, p1, p2)
    }

    override fun engineDoFinal(p0: ByteArray?, p1: Int, p2: Int, p3: ByteArray?, p4: Int): Int {
        TODO("Not yet implemented")
    }

    override fun engineUpdateAAD(src: ByteArray?, offset: Int, len: Int) {
        c.updateAAD(src, offset, len)
        authData = src
    }
}
