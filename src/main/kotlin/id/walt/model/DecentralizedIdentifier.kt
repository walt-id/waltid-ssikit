package id.walt.model

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import id.walt.common.prettyPrint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val DID_CONTEXT_URL: String = "https://w3id.org/did/v1"

enum class DidMethod {
    key,
    web,
    ebsi
}

abstract class BaseDid {
    abstract val id: String
    @Json(ignored = true) val url: DidUrl
        get() = DidUrl.from(id)
    @Json(ignored = true) val method: DidMethod
        get() = DidMethod.valueOf(url.method)

    fun encode() = Klaxon().toJsonString(this)
    fun encodePretty() = Klaxon().toJsonString(this).prettyPrint()

    companion object {
        fun decode(id: String, didDoc: String): BaseDid? {
            return when(DidUrl.from(id).method) {
                "key" -> Klaxon().parse<Did>(didDoc)
                "ebsi" -> Klaxon().parse<DidEbsi>(didDoc)
                // TODO: support did:web
                else -> null
            }
        }
    }
}

@Serializable
data class Did (
    @SerialName("@context")
    @Json(name = "@context")
    val context: String,
    override val id: String,
    @Json(serializeNull = false) val verificationMethod: List<VerificationMethod>? = null,
    @Json(serializeNull = false) val authentication: List<String>? = null,
    @Json(serializeNull = false) val assertionMethod: List<String>? = null,
    @Json(serializeNull = false) val capabilityDelegation: List<String>? = null,
    @Json(serializeNull = false) val capabilityInvocation: List<String>? = null,
    @Json(serializeNull = false) val keyAgreement: List<String>? = null,
    @Json(serializeNull = false) val serviceEndpoint: List<VerificationMethod>? = null,
) : BaseDid ()

@Serializable
data class VerificationMethod(
    val id: String,
    val type: String,
    val controller: String,
    @Json(serializeNull = false) val publicKeyBase58: String? = null,
    @Json(serializeNull = false) val publicKeyPem: String? = null,
    @Json(serializeNull = false) val publicKeyJwk: Jwk? = null
)

@Serializable
data class Jwk(
    @Json(serializeNull = false) var kid: String? = null, // "6a838696803b4140974a3d09b74ee6ec"
    @Json(serializeNull = false) val kty: String? = null, // "EC",
    @Json(serializeNull = false) val alg: String? = null, // "ES256K"
    @Json(serializeNull = false) val crv: String? = null, // "secp256k1",
    @Json(serializeNull = false) val use: String? = null, // "sig"
    @Json(serializeNull = false) val x: String? = null, // "Ou6y1zrJBeVnpV739kcTyez7RmQZFYg3F9bWGm6V5dQ",
    @Json(serializeNull = false) val y: String? = null, // "jOq6B8CsOxoXj-WXAGY28PH0Ype1x6bnOB6_YOo3lK0"
    @Json(serializeNull = false) val d: String? = null // HQCAQEEILZCiMcEeFuVLrciYxycmvTXffR
)

@Serializable
data class Service(
    val id: String,
    val type: String,
    val serviceEndpoint: String
)
