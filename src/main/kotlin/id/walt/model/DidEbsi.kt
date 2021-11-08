package id.walt.model

import com.beust.klaxon.*
import id.walt.vclib.model.Proof

data class DidEbsi (
    @Json(name = "@context")
    val context: EbsiContext,
    override val id: String,
    @Json(serializeNull = false) val verificationMethod: List<VerificationMethod>? = null,
    @Json(serializeNull = false) val authentication: List<String>? = null,
    @Json(serializeNull = false) var assertionMethod: List<String>? = null,
    @Json(serializeNull = false) val capabilityDelegation: List<String>? = null,
    @Json(serializeNull = false) val capabilityInvocation: List<String>? = null,
    @Json(serializeNull = false) val keyAgreement: List<String>? = null,
    @Json(serializeNull = false) val serviceEndpoint: List<VerificationMethod>? = null,
    @Json(serializeNull = false) var proof: Proof? = null,
) : BaseDid()

open class EbsiContext

class EbsiContextStr(val value: String): EbsiContext()

class EbsiContextList(val value: List<String>): EbsiContext()

class ContextConverter: Converter {
    override fun canConvert(cls: Class<*>): Boolean {
        return cls == EbsiContext::class.java
    }

    override fun fromJson(jv: JsonValue): Any? {
        val context = jv.inside
        return when (context) {
            is JsonArray<*> -> {
                EbsiContextList(context.value as List<String>)
            }
            is String -> {
                EbsiContextStr(context)
            }
            else -> {
                "Not allowed context format"
            }
        }
    }

    override fun toJson(value: Any): String {
        val ebsiContext = value as EbsiContext
        lateinit var ebsiContextStr: String
        return if (ebsiContext is EbsiContextList) ebsiContext.value.joinToString("\", \"", "[\"", "\"]", ) else "\"${(ebsiContext as EbsiContextStr).value}\""
    }
}