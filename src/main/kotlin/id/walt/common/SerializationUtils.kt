package id.walt.common

import com.beust.klaxon.*
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.model.VerificationMethod
import id.walt.sdjwt.SDMap
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.time.Instant

@Target(AnnotationTarget.FIELD)
annotation class VCList

@Target(AnnotationTarget.FIELD)
annotation class VCObjectList

@Target(AnnotationTarget.FIELD)
annotation class ListOrSingleVC

@Target(AnnotationTarget.FIELD)
annotation class ListOrSingleVCObject

@Target(AnnotationTarget.FIELD)
annotation class SingleVC

@Target(AnnotationTarget.FIELD)
annotation class SingleVCObject

@Target(AnnotationTarget.FIELD)
annotation class JsonObjectField


@Target(AnnotationTarget.FIELD)
annotation class KotlinxJsonObjectField

@Target(AnnotationTarget.FIELD)
annotation class ListOrSingleValue

@Target(AnnotationTarget.FIELD)
annotation class DidVerificationRelationships

@Target(AnnotationTarget.FIELD)
annotation class SDMapProperty

@Target(AnnotationTarget.FIELD)
annotation class InstantValue

class VcConverter(private val singleVC: Boolean, private val singleIfOne: Boolean, private val toVcObject: Boolean) :
    Converter {
    override fun canConvert(cls: Class<*>) = singleVC && cls == VerifiableCredential::class.java || cls == List::class.java

    override fun fromJson(jv: JsonValue): Any? {
        return if (singleVC) {
            (jv.string ?: jv.obj?.toJsonString())?.toVerifiableCredential()
        } else {
            (jv.array ?: listOf(jv.inside)).map {
                when (it) {
                    is JsonBase -> it.toJsonString()
                    else -> it.toString()
                }.toVerifiableCredential()
            }
        }
    }

    private fun toVcJsonString(vc: VerifiableCredential): String {
        return if (toVcObject) {
            vc.toJson()
        } else {
            vc.toJsonElement().toString()
        }
    }

    override fun toJson(value: Any): String {
        return if (singleVC) {
            toVcJsonString(value as VerifiableCredential)
        } else {
            if ((value as List<*>).size == 1 && singleIfOne) {
                toVcJsonString(value.first() as VerifiableCredential)
            } else {
                value.joinToString(",", "[", "]") { c ->
                    toVcJsonString(c as VerifiableCredential)
                }
            }
        }
    }

}

val jsonObjectFieldConverter = object : Converter {
    override fun canConvert(cls: Class<*>): Boolean {
        return cls == JsonObject::class.java
    }

    override fun fromJson(jv: JsonValue): Any? {
        return jv.obj
    }

    override fun toJson(value: Any): String {
        return Klaxon().toJsonString(value)
    }
}

val kotlinxJsonObjectFieldConverter = object : Converter {
    override fun canConvert(cls: Class<*>) = cls == kotlinx.serialization.json.JsonObject::class.java

    override fun fromJson(jv: JsonValue): Any? {
        return jv.obj?.toJsonString()?.let { Json.parseToJsonElement(it) }?.jsonObject
    }

    override fun toJson(value: Any): String {
        return (value as kotlinx.serialization.json.JsonObject).toString()
    }

}

val listOrSingleValueConverter = object : Converter {
    override fun canConvert(cls: Class<*>) = cls == List::class.java

    override fun fromJson(jv: JsonValue) =
        jv.array ?: listOf(jv.inside)

    override fun toJson(value: Any) = when ((value as List<*>).size) {
        1 -> Klaxon().toJsonString(value.first())
        else -> Klaxon().toJsonString(value)
    }
}

val didVerificationRelationshipsConverter = object : Converter {
    override fun canConvert(cls: Class<*>) = cls == List::class.java

    override fun fromJson(jv: JsonValue): Any? {
        jv.array ?: return null
        return jv.array!!.map { item ->
            when (item) {
                is String -> VerificationMethod.Reference(item)
                is JsonObject -> Klaxon().parseFromJsonObject<VerificationMethod>(item)
                else -> throw IllegalArgumentException("Verification relationship must be either String or JsonObject")
            }
        }
    }

    override fun toJson(value: Any): String {
        @Suppress("UNCHECKED_CAST")
        return (value as List<VerificationMethod>).joinToString(",", "[", "]") { item ->
            Klaxon().toJsonString(
                when {
                    item.isReference -> item.id
                    else -> item
                }
            )
        }
    }
}

val sdMapConverter = object : Converter {
    override fun canConvert(cls: Class<*>) = cls == SDMap::class.java

    override fun fromJson(jv: JsonValue): Any? {
        return jv.obj?.toJsonString()?.let { SDMap.fromJSON(it) }
    }

    override fun toJson(value: Any): String {
        return (value as SDMap).toJSON().toString()
    }

}

val klaxonInstantValueConverter = object : Converter {
    override fun canConvert(cls: Class<*>) = cls == InstantValue::class.java

    override fun fromJson(jv: JsonValue): Any? = jv.longValue ?: jv.int?.toLong()?.let {
        Instant.ofEpochSecond(it)
    }

    override fun toJson(value: Any): String {
        return (value as Instant).epochSecond.toString()
    }


}

fun KlaxonWithConverters() = Klaxon()
    .fieldConverter(VCList::class, VcConverter(singleVC = false, singleIfOne = false, toVcObject = false))
    .fieldConverter(VCObjectList::class, VcConverter(singleVC = false, singleIfOne = false, toVcObject = true))
    .fieldConverter(ListOrSingleVC::class, VcConverter(singleVC = false, singleIfOne = true, toVcObject = false))
    .fieldConverter(ListOrSingleVCObject::class, VcConverter(singleVC = false, singleIfOne = true, toVcObject = true))
    .fieldConverter(SingleVC::class, VcConverter(singleVC = true, singleIfOne = false, toVcObject = false))
    .fieldConverter(SingleVCObject::class, VcConverter(singleVC = true, singleIfOne = false, toVcObject = true))
    .fieldConverter(ListOrSingleValue::class, listOrSingleValueConverter)
    .fieldConverter(JsonObjectField::class, jsonObjectFieldConverter)
    .fieldConverter(DidVerificationRelationships::class, didVerificationRelationshipsConverter)
    .fieldConverter(SDMapProperty::class, sdMapConverter)
    .fieldConverter(KotlinxJsonObjectField::class, kotlinxJsonObjectFieldConverter)
    .fieldConverter(InstantValue::class, klaxonInstantValueConverter)

@Deprecated("Use KlaxonWithConverters()")
val KlaxonWithConverters = KlaxonWithConverters()
