package id.walt.model

import com.beust.klaxon.*
import id.walt.common.prettyPrint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

const val DID_CONTEXT_URL: String = "https://www.w3.org/ns/did/v1"

@Target(AnnotationTarget.FIELD)
annotation class ListOrSingleValue

@Target(AnnotationTarget.FIELD)
annotation class DidVerificationRelationships

val listOrSingleValueConverter = object: Converter {
    override fun canConvert(cls: Class<*>) = cls == List::class.java

    override fun fromJson(jv: JsonValue) =
        if (jv.array == null) {
            listOf(jv.inside)
        } else {
            jv.array
        }

    override fun toJson(value: Any)
        = when((value as List<*>).size) {
            1 -> Klaxon().toJsonString(value.first())
            else -> Klaxon().toJsonString(value)
        }
}

val verificationRelationshipsConverter = object: Converter {
    override fun canConvert(cls: Class<*>) = cls == List::class.java

    override fun fromJson(jv: JsonValue): Any? {
        if(jv.array != null) {
            return jv.array!!.map { item ->
                when(item) {
                    is String -> VerificationMethod.Reference(item)
                    is JsonObject -> Klaxon().parseFromJsonObject<VerificationMethod>(item)
                    else -> throw Exception("Verification relationship must be either String or JsonObject")
                }
            }
        }
        return null
    }

    override fun toJson(value: Any): String {
        return (value as List<VerificationMethod>).map { item -> if(item.isReference) {
                Klaxon().toJsonString(item.id)
            } else {
                Klaxon().toJsonString(item)
            }
        }.joinToString(",", "[", "]")
    }
}

val didSerializer = Klaxon().fieldConverter(ListOrSingleValue::class, listOrSingleValueConverter).fieldConverter(DidVerificationRelationships::class, verificationRelationshipsConverter)

class DidTypeAdapter : TypeAdapter<Did> {
    override fun classFor(type: Any): KClass<out Did> = when(DidUrl.from(type.toString()).method) {
        DidMethod.key.name -> DidKey::class
        DidMethod.ebsi.name -> DidEbsi::class
        DidMethod.web.name -> DidWeb::class
        DidMethod.iota.name -> DidIota::class
        else -> Did::class
    }
}

enum class DidMethod {
    key,
    web,
    ebsi,
    iota
}
@Serializable
@TypeFor(field = "id", adapter = DidTypeAdapter::class)
open class Did (
    @SerialName("@context")
    @Json(name = "@context")
    @ListOrSingleValue
    val context: List<String>,
    val id: String,
    @Json(serializeNull = false) var verificationMethod: List<VerificationMethod>? = null,
    @Json(serializeNull = false) @DidVerificationRelationships var authentication: List<VerificationMethod>? = null,
    @Json(serializeNull = false) @DidVerificationRelationships var assertionMethod: List<VerificationMethod>? = null,
    @Json(serializeNull = false) @DidVerificationRelationships var capabilityDelegation: List<VerificationMethod>? = null,
    @Json(serializeNull = false) @DidVerificationRelationships var capabilityInvocation: List<VerificationMethod>? = null,
    @Json(serializeNull = false) @DidVerificationRelationships var keyAgreement: List<VerificationMethod>? = null,
    @Json(serializeNull = false) var serviceEndpoint: List<ServiceEndpoint>? = null //TODO change to service-endpoint
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
        serviceEndpoint: List<ServiceEndpoint>? = null
    ) : this(listOf(context), id, verificationMethod, authentication, assertionMethod, capabilityDelegation, capabilityInvocation, keyAgreement, serviceEndpoint) {
    }

    @Json(ignored = true) val url: DidUrl
        get() = DidUrl.from(id)
    @Json(ignored = true) val method: DidMethod
        get() = DidMethod.valueOf(url.method)

    fun encode() = didSerializer.toJsonString(this)
    fun encodePretty() = didSerializer.toJsonString(this).prettyPrint()

    companion object {
        fun decode(didDoc: String): Did? {
            return didSerializer.parse<Did>(didDoc)
        }
    }
}

@Serializable
data class VerificationMethod(
    val id: String,
    val type: String,
    val controller: String,
    @Json(serializeNull = false) val publicKeyBase58: String? = null,
    @Json(serializeNull = false) val publicKeyPem: String? = null,
    @Json(serializeNull = false) val publicKeyJwk: Jwk? = null,
    @Json(serializeNull = false) val publicKeyMultibase: String? = null,
    @Json(serializeNull = false) val ethereumAddress: String? = null,
    @Json(ignored = true) val isReference: Boolean = false,
) {
    companion object {
        fun Reference(id: String): VerificationMethod {
            return VerificationMethod(id, "", "", isReference = true)
        }
    }

    fun toReference(): VerificationMethod {
        return VerificationMethod.Reference(id)
    }
}

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
