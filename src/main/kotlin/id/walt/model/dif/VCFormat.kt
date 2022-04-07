package id.walt.model.dif

data class VCFormatDefinition (
  val alg: Set<String>? = null,
  val proof_type: Set<String>? = null
    )

data class VCFormat (
  val jwt: VCFormatDefinition? = null,
  val jwt_vc: VCFormatDefinition? = null,
  val jwt_vp: VCFormatDefinition? = null,
  val ldp_vc: VCFormatDefinition? = null,
  val ldp_vp: VCFormatDefinition? = null,
  val ldp: VCFormatDefinition? = null
)
