package org.letstrust.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class DidEbsi(
    @SerialName("@context")
    val context: List<String>,
    var id: String? = null,
    val verificationMethod: List<VerificationMethod>? = null,
    val authentication: List<String>? = null,
    val assertionMethod: List<String>? = null,
    val capabilityDelegation: List<String>? = null,
    val capabilityInvocation: List<String>? = null,
    val keyAgreement: List<String>? = null,
    val serviceEndpoint: List<VerificationMethod>? = null,
    var proof: Proof? = null,
)

fun DidEbsi.encode() = Json.encodeToString(this)
fun DidEbsi.encodePretty() = Json { prettyPrint = true }.encodeToString(this)
