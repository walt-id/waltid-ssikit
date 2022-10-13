package id.walt.model.oidc

const val JWT_PROOF_TYPE = "jwt"

data class JwtProof(
    val jwt: String
) {
    val proof_type
        get() = JWT_PROOF_TYPE
}
