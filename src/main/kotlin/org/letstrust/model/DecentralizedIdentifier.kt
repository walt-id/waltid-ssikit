package org.letstrust.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val DID_CONTEXT_URL: String = "https://w3id.org/did/v1"

enum class DidMethod {
    key,
    web,
    ebsi
}

@Serializable
data class Did(
    @SerialName("@context")
    val context: String,
    var id: String? = null,
    val verificationMethod: List<VerificationMethod>? = null,
    val authentication: List<String>? = null,
    val assertionMethod: List<String>? = null,
    val capabilityDelegation: List<String>? = null,
    val capabilityInvocation: List<String>? = null,
    val keyAgreement: List<String>? = null,
    val serviceEndpoint: List<VerificationMethod>? = null,
)

fun Did.encode() = Json.encodeToString(this)
fun Did.encodePretty() = Json { prettyPrint = true }.encodeToString(this)
fun String.decode() = Json.decodeFromString<Did>(this)

@Serializable
data class VerificationMethod(
    val id: String,
    val type: String,
    val controller: String,
    val publicKeyBase58: String? = null,
    val publicKeyPem: String? = null,
    val publicKeyJwk: Jwk? = null
)

@Serializable
data class Jwk(
    val kty: String, // "EC",
    val crv: String, // "secp256k1",
    val x: String, // "Ou6y1zrJBeVnpV739kcTyez7RmQZFYg3F9bWGm6V5dQ",
    val y: String // "jOq6B8CsOxoXj-WXAGY28PH0Ype1x6bnOB6_YOo3lK0"
)

@Serializable
data class Service(
    val id: String,
    val type: String,
    val serviceEndpoint: String
)
