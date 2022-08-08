package id.walt.services.velocitynetwork.models.requests

import kotlinx.serialization.Serializable

@Serializable
data class CreateTenantRequest(
    val serviceIds: List<String>,
    val did: String,
    val keys: List<Key>,
){
    @Serializable
    data class Key(
        val purposes: List<String>,
        val algorithm: String,
        val encoding: String,
        val kidFragment: String,
        val key: String,
    )
}
