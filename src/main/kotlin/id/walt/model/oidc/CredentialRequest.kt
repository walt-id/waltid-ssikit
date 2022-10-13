package id.walt.model.oidc

data class CredentialRequest(
    val type: String,
    val format: String? = null,
    val proof: JwtProof? = null
)
