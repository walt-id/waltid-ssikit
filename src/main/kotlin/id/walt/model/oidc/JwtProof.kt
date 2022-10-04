package id.walt.model.oidc

data class JwtProof (
  val jwt: String
    ) {
  val proof_type
    get() = "jwt"
}