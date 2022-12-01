package id.walt.model.oidc

import id.walt.common.SingleVC
import id.walt.credentials.w3c.VerifiableCredential

data class CredentialResponse(
    val format: String?,
    @SingleVC val credential: VerifiableCredential?,
    val acceptance_token: String? = null,
    val c_nonce: String? = null,
    val c_nonce_expires_in: Int? = null
)
