package id.walt.model.oidc

data class CredentialResponse (
    val format: String?,
    val credential: String?,
    val acceptance_token: String? = null,
    val c_nonce: String? = null,
    val c_nonce_expires_in: Int? = null
        )
