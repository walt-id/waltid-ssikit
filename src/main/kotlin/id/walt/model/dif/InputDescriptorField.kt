package id.walt.model.dif

data class InputDescriptorField (
  val path: List<String>,
  val id: String? = null,
  val purpose: String? = null,
  val filter: Map<String, Any>? = null
)
