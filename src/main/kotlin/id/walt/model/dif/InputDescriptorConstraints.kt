package id.walt.model.dif

data class InputDescriptorConstraints(
  val fields: List<InputDescriptorField>? = null,
  val limit_disclosure: DisclosureLimitation? = null
)
