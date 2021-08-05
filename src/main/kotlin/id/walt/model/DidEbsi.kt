package id.walt.model

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import id.walt.vclib.model.Proof

data class DidEbsi(
    @Json(name = "@context")
    val context: List<String>,
    @Json(serializeNull = false) var id: String? = null,
    @Json(serializeNull = false) val verificationMethod: List<VerificationMethod>? = null,
    @Json(serializeNull = false) val authentication: List<String>? = null,
    @Json(serializeNull = false) var assertionMethod: List<String>? = null,
    @Json(serializeNull = false) val capabilityDelegation: List<String>? = null,
    @Json(serializeNull = false) val capabilityInvocation: List<String>? = null,
    @Json(serializeNull = false) val keyAgreement: List<String>? = null,
    @Json(serializeNull = false) val serviceEndpoint: List<VerificationMethod>? = null,
    @Json(serializeNull = false) var proof: Proof? = null,
)

fun DidEbsi.encode() = Klaxon().toJsonString(this)
fun DidEbsi.encodePretty() = encode()
