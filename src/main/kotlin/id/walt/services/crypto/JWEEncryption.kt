package id.walt.services.crypto

import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.*
import com.nimbusds.jose.jwk.ECKey
import javax.crypto.KeyGenerator

object JWEEncryption {

    private val passphraseHeader = JWEHeader(JWEAlgorithm.PBES2_HS512_A256KW, EncryptionMethod.A256GCM)

    private val directHeader = JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A256GCM)
    private val directKeyBitLength = EncryptionMethod.A256GCM.cekBitLength()

    private val ecdsaHeader = JWEHeader(JWEAlgorithm.ECDH_ES_A256KW, EncryptionMethod.A256GCM)

    private const val SALT_LENGTH = 32
    private const val ITERATION_COUNT = 50000

    private fun encrypt(payload: Payload, header: JWEHeader, encryptor: JWEEncrypter): String = JWEObject(header, payload).run {
        encrypt(encryptor)
        serialize()
    }

    private fun decrypt(jwe: String, decryptor: JWEDecrypter): Payload = JWEObject.parse(jwe).run {
        decrypt(decryptor)
        payload
    }

    fun passphraseEncrypt(payload: Payload, key: ByteArray): String =
        encrypt(payload, passphraseHeader, PasswordBasedEncrypter(key, SALT_LENGTH, ITERATION_COUNT))

    fun passphraseDecrypt(jwe: String, key: ByteArray): Payload = decrypt(jwe, PasswordBasedDecrypter(key))


    fun generateDirectKey(): ByteArray = KeyGenerator.getInstance("AES").run {
        init(directKeyBitLength)
        generateKey().encoded
    }

    fun directEncrypt(payload: Payload, key: ByteArray): String = encrypt(payload, directHeader, DirectEncrypter(key))
    fun directDecrypt(jwe: String, key: ByteArray): Payload = decrypt(jwe, DirectDecrypter(key))

    fun asymmetricEncrypt(payload: Payload, key: ECKey): String = encrypt(payload, ecdsaHeader, ECDHEncrypter(key))
    fun asymmetricDecrypt(jwe: String, key: ECKey): Payload = decrypt(jwe, ECDHDecrypter(key))

}