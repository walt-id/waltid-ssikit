package id.walt.credentials.w3c.templates

import id.walt.credentials.w3c.VerifiableCredential

data class VcTemplate(
  val name: String,
  val template: VerifiableCredential? = null,
  val mutable: Boolean
)
