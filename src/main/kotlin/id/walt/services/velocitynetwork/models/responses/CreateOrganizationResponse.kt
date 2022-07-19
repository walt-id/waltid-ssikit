package id.walt.services.velocitynetwork.models.responses

import kotlinx.serialization.Serializable

@Serializable
data class CreateOrganizationResponse(
    val id: String,
    val didDoc: String,
    val keys: List<Key>,
    val authClients: List<AuthClient>,
) {

    @Serializable
    data class AuthClient(
        val type: String,
        val clientType: String,
        val clientId: String,
        val clientSecret: String,
        val serviceId: String,
    )

    @Serializable
    data class Key(
        val id: String,
        val purposes: List<String>,
        val key: String,
        val publicKey: String,
        val algorithm: String,
        val encoding: String,
        val controller: String,
    )
}