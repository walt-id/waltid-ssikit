package id.walt.model

import com.beust.klaxon.Json
import com.beust.klaxon.TypeAdapter
import com.beust.klaxon.TypeFor
import id.walt.common.DidVerificationRelationships
import id.walt.common.KlaxonWithConverters
import id.walt.common.ListOrSingleValue
import id.walt.common.prettyPrint
import id.walt.model.did.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

const val DID_CONTEXT_URL: String = "https://www.w3.org/ns/did/v1"

class DidTypeAdapter : TypeAdapter<Did> {
    override fun classFor(type: Any): KClass<out Did> =
        DidMethod.values()
            .firstOrNull { it.name == DidUrl.from(type.toString()).method.lowercase() }?.didClass
            ?: Did::class
}

enum class DidMethod(val didClass: KClass<out Did>) {
    key(DidKey::class),
    web(DidWeb::class),
    ebsi(DidEbsi::class),
    iota(DidIota::class),
    cheqd(DidCheqd::class),

    jwk(Did::class)
}

@Serializable
@TypeFor(field = "id", adapter = DidTypeAdapter::class)
open class Did(
    @SerialName("@context")
    @Json(name = "@context", serializeNull = false)
    @ListOrSingleValue
    val context: List<String>? = null,
    val id: String,
    @Json(serializeNull = false) var verificationMethod: List<VerificationMethod>? = null,
    @Json(serializeNull = false) @DidVerificationRelationships open var authentication: List<VerificationMethod>? = null,
    @Json(serializeNull = false) @DidVerificationRelationships var assertionMethod: List<VerificationMethod>? = null,
    @Json(serializeNull = false) @DidVerificationRelationships var capabilityDelegation: List<VerificationMethod>? = null,
    @Json(serializeNull = false) @DidVerificationRelationships var capabilityInvocation: List<VerificationMethod>? = null,
    @Json(serializeNull = false) @DidVerificationRelationships var keyAgreement: List<VerificationMethod>? = null,
    @Json(serializeNull = false) var service: List<ServiceEndpoint>? = null
) {
    constructor( // secondary constructor with context as string
        context: String,
        id: String,
        verificationMethod: List<VerificationMethod>? = null,
        authentication: List<VerificationMethod>? = null,
        assertionMethod: List<VerificationMethod>? = null,
        capabilityDelegation: List<VerificationMethod>? = null,
        capabilityInvocation: List<VerificationMethod>? = null,
        keyAgreement: List<VerificationMethod>? = null,
        service: List<ServiceEndpoint>? = null
    ) : this(
        context = listOf(context),
        id = id,
        verificationMethod = verificationMethod,
        authentication = authentication,
        assertionMethod = assertionMethod,
        capabilityDelegation = capabilityDelegation,
        capabilityInvocation = capabilityInvocation,
        keyAgreement = keyAgreement,
        service = service
    )

    @Json(ignored = true)
    val url: DidUrl
        get() = DidUrl.from(id)

    @Json(ignored = true)
    val method: DidMethod
        get() = DidMethod.valueOf(url.method)

    fun encode() = KlaxonWithConverters().toJsonString(this)
    fun encodePretty() = KlaxonWithConverters().toJsonString(this).prettyPrint()

    companion object {
        fun decode(didDoc: String): Did? = KlaxonWithConverters().parse<Did>(didDoc)
    }

    override fun toString() = "[DidDocument of ${url.url}]"
}

@Serializable
data class VerificationMethod(
    val id: String,
    val type: String,
    val controller: String,
    @Json(serializeNull = false) val publicKeyBase58: String? = null,
    @Json(serializeNull = false) val publicKeyPem: String? = null,
    @Json(serializeNull = false) var publicKeyJwk: Jwk? = null,
    @Json(serializeNull = false) val publicKeyMultibase: String? = null,
    @Json(serializeNull = false) val ethereumAddress: String? = null,
    @Json(ignored = true) val isReference: Boolean = false,
) {
    companion object {
        fun Reference(id: String) = VerificationMethod(id, "", "", isReference = true)
    }

    fun toReference(): VerificationMethod = Reference(id)
}

@Serializable
data class Jwk(
    @Json(serializeNull = false) var kid: String? = null, // "6a838696803b4140974a3d09b74ee6ec"

    //@Json(serializeNull = false) val KeyType kty = null,
    @Json(serializeNull = false) val kty: String? = null, // "EC",

    // @Json(serializeNull = false) val Algorithm alg = null,
    @Json(serializeNull = false) val alg: String? = null, // "ES256K"
    @Json(serializeNull = false) val crv: String? = null, // "secp256k1",

    // @Json(serializeNull = false) val KeyUse use = null,
    @Json(serializeNull = false) val use: String? = null, // "sig"
    @Json(serializeNull = false) val x: String? = null, // "Ou6y1zrJBeVnpV739kcTyez7RmQZFYg3F9bWGm6V5dQ",
    @Json(serializeNull = false) val y: String? = null, // "jOq6B8CsOxoXj-WXAGY28PH0Ype1x6bnOB6_YOo3lK0"
    @Json(serializeNull = false) val d: String? = null, // HQCAQEEILZCiMcEeFuVLrciYxycmvTXffR

    @Json(serializeNull = false) val n: String? = null,
    @Json(serializeNull = false) val e: String? = null,


    // X509
    @Json(serializeNull = false) val x5u: String? = null


    //@Json(serializeNull = false) val Set<KeyOperation> ops,
    //@Json(serializeNull = false) val List<Base64> x5c,
    //@Json(serializeNull = false) val KeyStore ks = null

    //@Json(serializeNull = false) val x5t: Base64URL = null,
    // @Json(serializeNull = false) val Base64URL x5t256 = null,
)

@Serializable
data class Service(
    val id: String,
    val type: String,
    val serviceEndpoint: String
)
