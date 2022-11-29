package id.walt.common

import com.beust.klaxon.*
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.model.VerificationMethod

@Target(AnnotationTarget.FIELD)
annotation class VCList

@Target(AnnotationTarget.FIELD)
annotation class ListOrSingleVC

@Target(AnnotationTarget.FIELD)
annotation class SingleVC

@Target(AnnotationTarget.FIELD)
annotation class JsonObjectField

@Target(AnnotationTarget.FIELD)
annotation class ListOrSingleValue

@Target(AnnotationTarget.FIELD)
annotation class DidVerificationRelationships

object vcListConverter : Converter {

    override fun canConvert(cls: Class<*>): Boolean {
        return cls.isAssignableFrom(List::class.java)
    }

    override fun fromJson(jv: JsonValue): Any? {
        if(jv.array != null) {
            val arr = jv.array
            return arr!!.map {
                when(it) {
                    is JsonObject -> it.toJsonString()
                    else -> it.toString()
                }.toVerifiableCredential()
            }.toList()
        } else {
            throw KlaxonException("Couldn't parse verifiable credentials list")
        }
    }

    override fun toJson(value: Any): String {
        if(value is List<*> && (value.isEmpty() || value.first() is VerifiableCredential)) {
            return value.joinToString(",", "[", "]") {
                (it as VerifiableCredential).toJsonElement().toString()
            }
        } else {
            throw KlaxonException("Couldn't convert verifiable credentials list to Json")
        }
    }
}

val listOrSingleVCConverter = object : Converter {
    override fun canConvert(cls: Class<*>) = cls == List::class.java

    override fun fromJson(jv: JsonValue) =
        (jv.array ?: listOf(jv.inside)).map {
            when (it) {
                is JsonBase -> it.toJsonString()
                else -> it.toString()
            }.toVerifiableCredential()
        }

    override fun toJson(value: Any) = when ((value as List<*>).size) {
        1 -> (value.first() as VerifiableCredential).toJsonElement().toString()
        else -> value.joinToString(",", "[", "]") { c ->
            (c as VerifiableCredential).toJsonElement().toString()
        }
    }
}

val singleVCConverter = object : Converter {
    override fun canConvert(cls: Class<*>) = cls == VerifiableCredential::class.java

    override fun fromJson(jv: JsonValue) = (jv.string ?: jv.obj?.toJsonString())?.toVerifiableCredential()

    override fun toJson(value: Any): String {
        return (value as VerifiableCredential).toJsonElement().toString()
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
                else -> throw Exception("Verification relationship must be either String or JsonObject")
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

val klaxonWithConverters = Klaxon()
    .fieldConverter(VCList::class, vcListConverter)
    .fieldConverter(ListOrSingleValue::class, listOrSingleValueConverter)
    .fieldConverter(ListOrSingleVC::class, listOrSingleVCConverter)
    .fieldConverter(SingleVC::class, singleVCConverter)
    .fieldConverter(JsonObjectField::class, jsonObjectFieldConverter)
    .fieldConverter(DidVerificationRelationships::class, didVerificationRelationshipsConverter)
