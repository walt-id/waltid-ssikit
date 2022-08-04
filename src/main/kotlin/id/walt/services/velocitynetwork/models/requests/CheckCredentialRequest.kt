package id.walt.services.velocitynetwork.models.requests

import kotlinx.serialization.Serializable

@Serializable
data class CheckCredentialRequest(
    val rawCredentials: List<RawCredential>,
) {
    @Serializable
    data class RawCredential(
        val id: String,
        val rawCredential: String,
    )
}