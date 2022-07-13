package id.walt.services.velocitynetwork.models

import id.walt.model.Did

data class CreateOrganizationResponse(
    val id: String,
    val didDoc: String,
    val keys: List<Key>,
    val authClients: List<AuthClient>,
) {

    data class AuthClient(
        val type: String,
        val clientType: String,
        val clientId: String,
        val clientSecret: String,
        val serviceId: String,
    )

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