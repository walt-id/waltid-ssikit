package id.walt.crypto

import id.walt.services.crypto.CryptoService
import java.security.AlgorithmParameters
import java.security.Key
import java.security.SecureRandom
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.CipherSpi
import javax.crypto.spec.GCMParameterSpec

open class LtCipherSpi(val algorithm: String) : CipherSpi() {

    val c: Cipher = Cipher.getInstance(algorithm)

    private val cryptoService = CryptoService.getService()

    class AES256_GCM_NoPadding : LtCipherSpi("AES/GCM/NoPadding")

    var isEncryptionMode = true
    var iv: ByteArray? = null
    var authData: ByteArray? = null
    var plainText: ByteArray? = null

    override fun engineSetMode(mode: String?) = TODO("Not yet implemented")

    override fun engineSetPadding(padding: String?) = TODO("Not yet implemented")

    override fun engineGetBlockSize(): Int = c.blockSize

    override fun engineGetOutputSize(inputLen: Int): Int = TODO("Not yet implemented")

    override fun engineGetIV(): ByteArray = TODO("Not yet implemented")

    override fun engineGetParameters(): AlgorithmParameters = c.parameters

    override fun engineInit(opmode: Int, key: Key?, random: SecureRandom?) = TODO("Not yet implemented")

    override fun engineInit(opmode: Int, key: Key?, params: AlgorithmParameterSpec?, random: SecureRandom?) {
        if (opmode == 2) isEncryptionMode = false
        iv = (params as GCMParameterSpec).iv
        c.init(opmode, key, params, random)
    }

    override fun engineInit(opmode: Int, key: Key?, params: AlgorithmParameters?, random: SecureRandom?) =
        TODO("Not yet implemented")

    override fun engineUpdate(input: ByteArray?, inputOffset: Int, inputLen: Int): ByteArray =
        TODO("Not yet implemented")

    override fun engineUpdate(
        input: ByteArray?,
        inputOffset: Int,
        inputLen: Int,
        output: ByteArray?,
        outputOffset: Int
    ): Int =
        TODO("Not yet implemented")

    override fun engineDoFinal(input: ByteArray?, inputOffset: Int, inputLen: Int): ByteArray {
        plainText = input
        if (isEncryptionMode) {
            return cryptoService.encrypt(KeyId("123"), algorithm, plainText!!, authData, iv)
        }
        return cryptoService.decrypt(KeyId("123"), algorithm, plainText!!, authData, iv)

        //    return c.doFinal(p0, p1, p2)
    }

    override fun engineDoFinal(
        input: ByteArray?,
        inputOffset: Int,
        inputLen: Int,
        output: ByteArray?,
        outputOffset: Int
    ): Int =
        TODO("Not yet implemented")

    override fun engineUpdateAAD(src: ByteArray?, offset: Int, len: Int) {
        c.updateAAD(src, offset, len)
        authData = src
    }
}
