package org.letstrust.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val DID_CONTEXT_URL: String = "https://w3id.org/did/v1"

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

@Serializable
data class VerificationMethod(
    val id: String,
    val type: String,
    val controller: String,
    val publicKeyBase58: String
)

@Serializable
data class Service(
    val id: String,
    val type: String,
    val serviceEndpoint: String
)
