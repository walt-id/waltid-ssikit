package org.letstrust.crypto

import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava
import io.ipfs.multibase.Base58
import io.ipfs.multibase.Multibase
import org.letstrust.CryptoProvider
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

// Supported key algorithms
enum class KeyAlgorithm {
    EdDSA_Ed25519,
    ECDSA_Secp256k1
}

enum class KeyFormat {
    PEM,
    BASE64
}

// Supported signatures
enum class SignatureType {
    Ed25519Signature2018,
    EcdsaSecp256k1Signature2019,
    Ed25519Signature2020
}

fun newKeyId(): KeyId = KeyId("LetsTrust-Key-${UUID.randomUUID().toString().replace("-", "")}")

// EdECPrivateKeySpec(ED25519, this.encoded
fun PrivateKey.toPEM(): String =
    "-----BEGIN PRIVATE KEY-----\n" +
            String(
                Base64.getMimeEncoder(64, "\n".toByteArray()).encode(PKCS8EncodedKeySpec(this.encoded).encoded)
            ) +
            "\n-----END PRIVATE KEY-----"


fun PrivateKey.toBase64(): String = String(Base64.getEncoder().encode(PKCS8EncodedKeySpec(this.encoded).encoded))

fun PublicKey.toPEM(): String = "-----BEGIN PUBLIC KEY-----\n" +
        String(
            Base64.getMimeEncoder(64, "\n".toByteArray()).encode(X509EncodedKeySpec(this.encoded).encoded)
        ) +
        "\n-----END PUBLIC KEY-----"

fun encBase64(bytes: ByteArray): String = String(Base64.getEncoder().encode(bytes))

fun decBase64(base64: String): ByteArray = Base64.getDecoder().decode(base64)

fun PublicKey.toBase64(): String = encBase64(X509EncodedKeySpec(this.encoded).encoded)

fun decodePubKeyBase64(base64: String, kf: KeyFactory): PublicKey = kf.generatePublic(X509EncodedKeySpec(decBase64(base64)))

fun decodePubKeyPem(pem: String, kf: KeyFactory): PublicKey = decodePubKeyBase64(pemToBase64(pem), kf)

fun pemToBase64(pem: String): String = pem.substringAfter("\n").substringBefore("-").replace("\n", "")

fun decodePrivKeyBase64(base64: String, kf: KeyFactory): PrivateKey = kf.generatePrivate(PKCS8EncodedKeySpec(decBase64(base64)))

fun decodePrivKeyPem(pem: String, kf: KeyFactory): PrivateKey = decodePrivKeyBase64(pemToBase64(pem), kf)

fun buildKey(keyId: String, algorithm: String, provider: String, publicPart: String, privatePart: String, format: KeyFormat = KeyFormat.PEM): Key {

    val kf = when (KeyAlgorithm.valueOf(algorithm)) {
        KeyAlgorithm.ECDSA_Secp256k1 -> KeyFactory.getInstance("ECDSA")
        KeyAlgorithm.EdDSA_Ed25519 -> KeyFactory.getInstance("Ed25519")
    }
    val kp = when (format) {
        KeyFormat.PEM -> Pair(decodePubKeyPem(publicPart, kf), decodePrivKeyPem(privatePart, kf))
        KeyFormat.BASE64 -> Pair(decodePubKeyBase64(publicPart, kf), decodePrivKeyPem(privatePart, kf))
    }

    return Key(KeyId(keyId), KeyAlgorithm.valueOf(algorithm), CryptoProvider.valueOf(provider), KeyPair(kp.first, kp.second))
}

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


fun keyPairGeneratorSecp256k1(): KeyPairGenerator {
    val kg = KeyPairGenerator.getInstance("EC", "BC")
    kg.initialize(ECGenParameterSpec("secp256k1"), SecureRandom())
    return kg
}

fun keyPairGeneratorEd25519(): KeyPairGenerator {
    return KeyPairGenerator.getInstance("Ed25519")
}

