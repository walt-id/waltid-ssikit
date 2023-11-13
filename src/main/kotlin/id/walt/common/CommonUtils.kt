package id.walt.common

import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.jwk.RSAKey
import id.walt.crypto.KeyAlgorithm
import id.walt.model.Jwk
import id.walt.services.WaltIdServices.httpNoAuth
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import org.apache.commons.codec.digest.DigestUtils
import org.bouncycastle.util.encoders.Base32
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import java.util.zip.*
import kotlin.reflect.full.memberProperties

fun resolveContent(fileUrlContent: String): String {
    val file = File(fileUrlContent)
    if (file.exists()) {
        return file.readText()
    }
    if (fileUrlContent.startsWith("class:")) {
        val enclosingClass = object {}.javaClass.enclosingClass
        val path = fileUrlContent.substring(6)
        var url = enclosingClass.getResource(path)
        if (url == null && !path.startsWith('/'))
            url = enclosingClass.getResource("/$path")
        return url?.readText() ?: fileUrlContent
    }
    if (Regex("^https?:\\/\\/.*$").matches(fileUrlContent)) {
        return runBlocking { httpNoAuth.get(fileUrlContent).bodyAsText() }
    }
    return fileUrlContent
}

fun resolveContentToFile(fileUrlContent: String, tempPrefix: String = "TEMP", tempPostfix: String = ".txt"): File {
    val fileCheck = File(fileUrlContent)
    if (!fileCheck.exists()) {
        File.createTempFile(tempPrefix, tempPostfix).let {
            it.writeText(resolveContent(fileUrlContent))
            return it
        }
    }
    return fileCheck
}

fun compressGzip(data: ByteArray): ByteArray {
    val result = ByteArrayOutputStream()
    GZIPOutputStream(result).use {
        it.write(data)
    }
    return result.toByteArray()
}

fun uncompressGzip(data: ByteArray, idx: ULong? = null) =
    GZIPInputStream(data.inputStream()).bufferedReader().use {
        idx?.let { index ->
            var int = it.read()
            var count = 0U
            var char = int.toChar()
            while (int != -1 && count++ <= index) {
                char = int.toChar()
                int = it.read()
            }
            char
        }?.let {
            val array = CharArray(1)
            array[0] = it
            array
        } ?: it.readText().toCharArray()
    }

fun buildRawBitString(bitSet: BitSet): ByteArray{
    var lastIndex = 0
    var currIndex = bitSet.nextSetBit(lastIndex);
    val builder = StringBuilder()
    while (currIndex > -1) {
        val delta = 1 % (lastIndex + 1)
        builder.append("0".repeat(currIndex - lastIndex - delta)).append("1")
        lastIndex = currIndex
        currIndex = bitSet.nextSetBit(lastIndex + 1)//TODO: handle overflow
    }
    builder.append("0".repeat(bitSet.size() - lastIndex - 1))
    return builder.toString().toByteArray()
}

fun createEncodedBitString(bitSet: BitSet = BitSet(16 * 1024 * 8)): ByteArray =
    Base64.getEncoder().encode(compressGzip(buildRawBitString(bitSet)))

fun decodeBitSet(bitString: String): BitSet = uncompressGzip(Base64.getDecoder().decode(bitString)).toBitSet(16 * 1024 * 8)

fun createBaseToken() = UUID.randomUUID().toString() + UUID.randomUUID().toString()
fun deriveRevocationToken(baseToken: String): String = Base32.toBase32String(DigestUtils.sha256(baseToken)).replace("=", "")

fun String.toBitSet(initialSize: Int) = let {
    val bitSet = BitSet(initialSize)
    for (i in this.indices) {
        if (this[i] == '1') bitSet.set(i)
    }
    bitSet
}

fun CharArray.toBitSet(initialSize: Int) = String(this).toBitSet(initialSize)

/**
 * Converts a class properties into map.
 *
 * ___Note___: Applicable only for linear properties, nested properties will be ignored.
 */
inline fun <reified T : Any> T.asMap() : Map<String, Any?> {
    val props = T::class.memberProperties.associateBy { it.name }
    return props.keys.associateWith { props[it]?.get(this) }
}

/**
 * Converts the JWK public key to a required-only members JSON string
 * @param - the JWK key
 * @return - the JSON string representing the public key having only the required members
 */
fun convertToRequiredMembersJsonString(jwk: JWK): Jwk = when (getKeyAlgorithm(jwk)) {
    KeyAlgorithm.ECDSA_Secp256k1, KeyAlgorithm.ECDSA_Secp256r1 -> EcPublicKeyRequiredMembers(jwk.toECKey())
    KeyAlgorithm.EdDSA_Ed25519 -> OkpPublicKeyRequiredMembers(jwk.toOctetKeyPair())
    KeyAlgorithm.RSA -> RsaPublicKeyRequiredMembers(jwk.toPublicJWK().toRSAKey())
}

fun getKeyAlgorithm(jwk: JWK): KeyAlgorithm = when (jwk.keyType.value.lowercase()) {
    "rsa" -> KeyAlgorithm.RSA
    "ec" -> when (jwk.toECKey().curve.stdName.lowercase()) {
        "secp256k1" -> KeyAlgorithm.ECDSA_Secp256k1
        "secp256r1" -> KeyAlgorithm.ECDSA_Secp256r1
        else -> throw IllegalArgumentException("Curve ${jwk.toECKey().curve.stdName} for EC algorithm not supported.")
    }

    "okp" -> KeyAlgorithm.EdDSA_Ed25519
    else -> throw IllegalArgumentException("Key algorithm ${jwk.keyType.value} not supported.")
}

private fun OkpPublicKeyRequiredMembers(okp: OctetKeyPair) = Jwk(
    crv = okp.curve.name,
    kty = okp.keyType.value,
    x = okp.x.toString()
)

private fun EcPublicKeyRequiredMembers(ec: ECKey) = Jwk(
    crv = ec.curve.name,
    kty = ec.keyType.value,
    x = ec.x.toString(),
    y = ec.y.toString()
)

private fun RsaPublicKeyRequiredMembers(rsa: RSAKey) = Jwk(
    e = rsa.publicExponent.toString(),
    kty = rsa.algorithm.name,
    n = rsa.modulus.toString()
)
