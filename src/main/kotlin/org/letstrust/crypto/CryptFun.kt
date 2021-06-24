package org.letstrust.crypto

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.SodiumJava
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.impl.ECDSA
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.util.Base64URL
import io.ipfs.multibase.Base58
import io.ipfs.multibase.Multibase
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.util.encoders.Hex
import org.letstrust.CryptoProvider
import org.letstrust.model.EncryptedAke1Payload
import org.web3j.crypto.ECDSASignature
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
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

fun newKeyId(): KeyId = KeyId(UUID.randomUUID().toString().replace("-", ""))

// EdECPrivateKeySpec(ED25519, this.encoded
fun PrivateKey.toPEM(): String =
    "-----BEGIN PRIVATE KEY-----\n" +
            String(
                Base64.getMimeEncoder(64, "\n".toByteArray()).encode(PKCS8EncodedKeySpec(this.encoded).encoded)
            ) +
            "\n-----END PRIVATE KEY-----"


fun PrivateKey.toBase64(): String = String(Base64.getEncoder().encode(PKCS8EncodedKeySpec(this.encoded).encoded))

fun java.security.Key.toPEM(): String = when {
    this is PublicKey -> this.toPEM()
    this is PrivateKey -> this.toPEM()
    else -> throw IllegalArgumentException()
}

fun PublicKey.toPEM(): String = "-----BEGIN PUBLIC KEY-----\n" +
        String(
            Base64.getMimeEncoder(64, "\n".toByteArray()).encode(X509EncodedKeySpec(this.encoded).encoded)
        ) +
        "\n-----END PUBLIC KEY-----"

fun encBase64Str(data: String): String = String(Base64.getEncoder().encode(data.toByteArray()))

fun decBase64Str(base64: String): String = String(Base64.getDecoder().decode(base64))

fun encBase64(bytes: ByteArray): String = String(Base64.getEncoder().encode(bytes))

fun decBase64(base64: String): ByteArray = Base64.getDecoder().decode(base64)

fun PublicKey.toBase64(): String = encBase64(X509EncodedKeySpec(this.encoded).encoded)

fun decodePubKeyBase64(base64: String, kf: KeyFactory): PublicKey = kf.generatePublic(X509EncodedKeySpec(decBase64(base64)))

fun decodePubKeyPem(pem: String, kf: KeyFactory): PublicKey = decodePubKeyBase64(pemToBase64(pem), kf)

fun pemToBase64(pem: String): String = pem.substringAfter("\n").substringBefore("-").replace("\n", "")

fun decodePrivKeyBase64(base64: String, kf: KeyFactory): PrivateKey = kf.generatePrivate(PKCS8EncodedKeySpec(decBase64(base64)))

fun decodePrivKeyPem(pem: String, kf: KeyFactory): PrivateKey = decodePrivKeyBase64(pemToBase64(pem), kf)

fun buildKey(keyId: String, algorithm: String, provider: String, publicPart: String, privatePart: String?, format: KeyFormat = KeyFormat.PEM): Key {

    val kf = when (KeyAlgorithm.valueOf(algorithm)) {
        KeyAlgorithm.ECDSA_Secp256k1 -> KeyFactory.getInstance("ECDSA")
        KeyAlgorithm.EdDSA_Ed25519 -> KeyFactory.getInstance("Ed25519")
    }
    val kp = when (format) {
        KeyFormat.PEM -> Pair(decodePubKeyPem(publicPart, kf), privatePart?.let { decodePrivKeyPem(privatePart, kf) })
        KeyFormat.BASE64 -> Pair(decodePubKeyBase64(publicPart, kf), privatePart?.let { decodePrivKeyPem(privatePart, kf) })
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

fun localTimeSecondsUtc(): String {
    val inDateTime = ZonedDateTime.of(LocalDateTime.now(), ZoneOffset.UTC)

    val inDateEpochSeconds = Instant.ofEpochSecond(inDateTime.toEpochSecond())

    return DateTimeFormatter.ISO_INSTANT.format(inDateEpochSeconds)
}

class SortingNodeFactory : JsonNodeFactory() {
    override fun objectNode(): ObjectNode =
        ObjectNode(this, TreeMap<String, JsonNode>())
}

val mapper: ObjectMapper = JsonMapper.builder()
    .nodeFactory(SortingNodeFactory())
    .build()

fun canonicalize(json: String): String =
    mapper.writeValueAsString(mapper.readTree(json))

fun uncompressSecp256k1(compKey: ByteArray?): ECKey? {
    val point: ECPoint = ECNamedCurveTable.getParameterSpec(Curve.SECP256K1.name).getCurve().decodePoint(compKey)

    val x: ByteArray = point.getXCoord().getEncoded()
    val y: ByteArray = point.getYCoord().getEncoded()

    return ECKey.Builder(Curve.SECP256K1, Base64URL.encode(x), Base64URL.encode(y)).build()
}

fun parseEncryptedAke1Payload(encryptedPayload: String): EncryptedAke1Payload {
    val bytes = Numeric.hexStringToByteArray(encryptedPayload)
    // https://bitcoinj.org/javadoc/0.15.10/org/bitcoinj/core/ECKey.html#decompress--
    val hexKey = org.bitcoinj.core.ECKey.fromPublicOnly(bytes.sliceArray(16..48)).decompress().publicKeyAsHex
    val jwkKey = uncompressSecp256k1(Hex.decode(hexKey))

    return EncryptedAke1Payload(
        bytes.sliceArray(0..15),
        jwkKey!!,
        bytes.sliceArray(49..80),
        bytes.sliceArray(81 until bytes.size)
    )

//    return EncryptedPayload(
//        Hex.toHexString(bytes.sliceArray(0..15)),
//        // https://bitcoinj.org/javadoc/0.15.10/org/bitcoinj/core/ECKey.html#decompress--
//        org.bitcoinj.core.ECKey.fromPublicOnly(bytes.sliceArray(16..48)).decompress().publicKeyAsHex,
//        Hex.toHexString(bytes.sliceArray(49..80)),
//        Hex.toHexString(bytes.sliceArray(81 until bytes.size))
//    )
}

// Returns the index of first match of the predicate or the full size of the array
inline fun ByteArray.findFirst(predicate: (Byte) -> Boolean): Int {
    for ((index, element) in this.withIndex()) {
        if (predicate(element)) return index
    }
    return size
}

fun toECDSASignature(jcaSignature: ByteArray, algorithm: JWSAlgorithm): ECDSASignature {
    val rsSignature = ECDSA.transcodeSignatureToConcat(jcaSignature, ECDSA.getSignatureByteArrayLength(algorithm))
    return ECDSASignature(
        BigInteger(1, rsSignature.copyOfRange(0, rsSignature.size / 2)),
        BigInteger(1, rsSignature.copyOfRange(rsSignature.size / 2, rsSignature.size))
    ).toCanonicalised()
}

fun toECDSASignatureAlt(jcaSignature: ByteArray): ECDSASignature {
    val rLength = jcaSignature[3].toInt()
    val sLength = jcaSignature[4 + rLength + 1].toInt()
    return ECDSASignature(
        BigInteger(1, jcaSignature.copyOfRange(4, 4 + rLength)),
        BigInteger(1, jcaSignature.copyOfRange(4 + rLength + 2, 4 + rLength + 2 + sLength))
    ).toCanonicalised()
}

////    print("jcaSignature as bytes: ");jcaSignature.forEach { print("\"$it\" ") }; println()
////    println("jcaSignature as hex  : ${Numeric.toHexStringNoPrefix(jcaSignature)}")
//    val rsSignature = ECDSA.transcodeSignatureToConcat(jcaSignature, ECDSA.getSignatureByteArrayLength(algorithm))
//
//    val r = ByteArray(rsSignature.size / 2)
//    val s = ByteArray(rsSignature.size / 2)
//    System.arraycopy(rsSignature, 0, r, 0, rsSignature.size / 2)
//    System.arraycopy(rsSignature, rsSignature.size / 2, s, 0, rsSignature.size / 2)
//
//    return ECDSASignature(
//        BigInteger(1, r),
//        BigInteger(1, s)
//    ).toCanonicalised()
//          32                                                                  33
//    304502202adce0c348a461fcd0af4b5d3f9d8db335381d1f56fe01646a3f338f8e9dff9e022100f5bbdc9a3f4a6958e3377a12f60939bfaf6e5941ad68f28a6b23147479f86ab4
//    304402206878b5690514437a2342405029426cc2b25b4a03fc396fef845d656cf62bad2c022018610a8d37e3384245176ab49ddbdbe8da4133f661bf5ea7ad4e3d2b912d856f01


//    new BigInteger(s,16)
//    println("jcaSignature:")
//    println("30: ${String.format("%02x", jcaSignature[0])}")
//    println("length z: ${String.format("%02x", jcaSignature[1])}")
//    println("02: ${String.format("%02x", jcaSignature[2])}")
//    println("length r: ${String.format("%02x", jcaSignature[3])}")
//    println("02: ${String.format("%02x", jcaSignature[3 + Integer.parseInt(String.format("%02x", jcaSignature[3])) + 1])}")
//    println("length s: ${String.format("%02x", jcaSignature[3 + Integer.parseInt(String.format("%02x", jcaSignature[3])) + 2])}")
//    jcaSignature.forEach { print(String.format("%02x", it)) }; println()
//    println(Numeric.toHexStringNoPrefix(jcaSignature).slice(8..72))
//    304402206878b5690514437a2342405029426cc2b25b4a03fc396fef845d656cf62bad2c022018610a8d37e3384245176ab49ddbdbe8da4133f661bf5ea7ad4e3d2b912d856f01
//    304502203ce24f0798a42a9a1e7957e158b5ddc92c9d2cc2a377b9242c8aeec1bde690f3022100bceda98084dc3718514757cb82a48a176b5c280f2894210a21312b4592be577f
//    3046022100cf22fe5816239f756369b1884ed8da2e4190cc1a1afa62dc8a12cd56b7156951022100def4d7040ba158407d121ab19bad2a1bc7bf2a70b7561a15faec9aadbf5e2d40
//    print("r (old) = "); r.forEach { print(it) }; println()
//    print("r (new) = "); jcaSignature.copyOfRange(4, 4+32+1).forEach { print(it) }; println()
//    print("s (old) = "); s.forEach { print(it) }; println()
//    print("s (new) = "); jcaSignature.copyOfRange(4+32+1, 4+32+1+32+1).forEach { print(it) }; println()
