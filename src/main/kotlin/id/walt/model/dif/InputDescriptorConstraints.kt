package id.walt.model.dif

import com.beust.klaxon.Json

data class InputDescriptorConstraints(
  val fields: List<InputDescriptorField>? = null,
  @Json(serializeNull = false) val limit_disclosure: DisclosureLimitation? = null
)
