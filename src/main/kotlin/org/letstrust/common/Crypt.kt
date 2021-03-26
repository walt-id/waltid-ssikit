package org.letstrust

import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava
import io.ipfs.multibase.Base58
import io.ipfs.multibase.Multibase
import org.letstrust.crypto.KeyId
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*


enum class KeyAlgorithm {
    Ed25519,
    Secp256k1
}

fun newKeyId(): KeyId = KeyId("LetsTrust-Key-${UUID.randomUUID().toString().replace("-", "")}")


fun PrivateKey.toPEM(): String =
    "-----BEGIN PRIVATE KEY-----\n" +
            String(
                Base64.getMimeEncoder(64, "\n".toByteArray()).encode(PKCS8EncodedKeySpec(this.encoded).encoded)
            ) +
            "\n-----END PRIVATE KEY-----"


fun PrivateKey.toBase64(): String = String(Base64.getEncoder().encode(PKCS8EncodedKeySpec(this.encoded).encoded))

fun PrivateKey.toJwk(): String = "todo"

fun PublicKey.toPEM(): String = "-----BEGIN PUBLIC KEY-----\n" +
        String(
            Base64.getMimeEncoder(64, "\n".toByteArray()).encode(X509EncodedKeySpec(this.encoded).encoded)
        ) +
        "\n-----END PUBLIC KEY-----"

fun PublicKey.toJwk(): String = "todo"

fun PublicKey.toBase64(): String = String(Base64.getEncoder().encode(X509EncodedKeySpec(this.encoded).encoded))

fun decodePubKey(s: String, kf: KeyFactory): PublicKey = kf.generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(s)))

fun decodePrivKey(s: String, kf: KeyFactory): PrivateKey = kf.generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(s)))

fun ByteArray.encodeBase58(): String = Base58.encode(this)

fun String.decodeBase58(): ByteArray = Base58.decode(this)

fun ByteArray.encodeMultiBase58Btc(): String = Multibase.encode(Multibase.Base.Base58BTC, this)

fun String.decodeMultiBase58Btc(): ByteArray = Multibase.decode(this)

fun ByteArray.toHexString() = this.joinToString("") { String.format("%02X", (it.toInt() and 0xFF)) }

fun String.byteArrayFromHexString() = this.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

fun convertEd25519PublicKeyFromMultibase58Btc(mbase58: String): ByteArray {

    if (mbase58[0] != 'z') throw RuntimeException("Invalid multibase encoding of ED25519 key")

    val buffer = mbase58.substring(1).decodeBase58()

    // Ed25519 public key - https://github.com/multiformats/multicodec#adding-new-multicodecs-to-the-table
    if (!(0xed.toByte() == buffer[0] && 0x01.toByte() == buffer[1])) throw RuntimeException("Invalid cryptonym encoding of ED25519 key")

    return buffer.copyOfRange(2, buffer.size)
}

fun convertX25519PublicKeyFromMultibase58Btc(mbase58: String): ByteArray {

    if (mbase58[0] != 'z') throw RuntimeException("Invalid multibase encoding of ED25519 key")

    val buffer = mbase58.substring(1).decodeBase58()
    println(buffer.toHexString())
    if (!(0xec.toByte() == buffer[0] && 0x01.toByte() == buffer[1])) throw RuntimeException("Invalid cryptonym encoding of Curve25519 key")

    return buffer.copyOfRange(2, buffer.size)
}

fun convertEd25519PublicKeyToMultiBase58Btc(edPublicKey: ByteArray): String {
    val edPublicKeyCryptonym = ByteArray(edPublicKey.size + 2)
    edPublicKeyCryptonym[0] = 0xed.toByte() // Ed25519 public key
    edPublicKeyCryptonym[1] = 0x01.toByte()
    edPublicKey.copyInto(edPublicKeyCryptonym, 2)
    return edPublicKeyCryptonym.encodeMultiBase58Btc()
}

fun convertX25519PublicKeyToMultiBase58Btc(x25519PublicKey: ByteArray): String {
    val dhPublicKeyCryptonym = ByteArray(x25519PublicKey.size + 2)
    dhPublicKeyCryptonym[0] = 0xec.toByte() // Curve25519 public key
    dhPublicKeyCryptonym[1] = 0x01.toByte()
    x25519PublicKey.copyInto(dhPublicKeyCryptonym, 2)
    return dhPublicKeyCryptonym.encodeMultiBase58Btc()
}

// https://libsodium.gitbook.io/doc/advanced/ed25519-curve25519
// https://blog.mozilla.org/warner/2011/11/29/ed25519-keys/
// https://github.com/datkt/sodium
fun convertPublicKeyEd25519ToCurve25519(ed25519PublicKey: ByteArray): ByteArray {
    // https://libsodium.gitbook.io/doc/advanced/ed25519-curve25519
    val lazySodium = LazySodiumJava(SodiumJava())
    val dhPublicKey = ByteArray(32)
    if (!lazySodium.convertPublicKeyEd25519ToCurve25519(
            dhPublicKey,
            ed25519PublicKey
        )
    ) throw RuntimeException("Could not convert Ed25519 to X25519 pubic key")
    return dhPublicKey
}
