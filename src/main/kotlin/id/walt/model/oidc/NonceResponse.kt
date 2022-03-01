package id.walt.model.oidc

data class NonceResponse (
  val p_nonce: String,
  val expires_in: String? = null
    )
