package id.walt.model.oidc

import com.beust.klaxon.*
import id.walt.model.ListOrSingleValue
import id.walt.model.listOrSingleValueConverter
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.model.toCredential

@Target(AnnotationTarget.FIELD)
annotation class ListOrSingleVC

@Target(AnnotationTarget.FIELD)
annotation class SingleVC

val listOrSingleVCConverter = object: Converter {
  override fun canConvert(cls: Class<*>) = cls == List::class.java

  override fun fromJson(jv: JsonValue) =
    (jv.array ?: listOf(jv.inside))?.map {
      when(it) {
        is JsonBase -> it.toJsonString()
        else -> it.toString()
      }.toCredential()
    }

  override fun toJson(value: Any)
      = when((value as List<*>).size) {
    1 -> (value.first() as VerifiableCredential).encode()
    else -> value.map { c -> when((c as VerifiableCredential).jwt) {
      null -> c.encode()
      else -> "\"${c.encode()}\""
    } }.joinToString(",", "[", "]")
  }
}

val singleVCConverter = object: Converter {
  override fun canConvert(cls: Class<*>) = cls == VerifiableCredential::class.java

  override fun fromJson(jv: JsonValue) = (jv.string ?: jv.obj?.toJsonString())?.toCredential()

  override fun toJson(value: Any): String {
    return when((value as VerifiableCredential).jwt) {
      null -> value.encode()
      else -> "\"${value.encode()}\""
    }
  }
}

val klaxon = Klaxon().fieldConverter(ListOrSingleValue::class, listOrSingleValueConverter).fieldConverter(ListOrSingleVC::class, listOrSingleVCConverter).fieldConverter(SingleVC::class, singleVCConverter)
