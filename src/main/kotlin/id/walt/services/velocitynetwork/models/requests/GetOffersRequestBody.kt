package id.walt.services.velocitynetwork.models.requests

import kotlinx.serialization.Serializable

@Serializable
data class GetOffersRequestBody(
    val exchangeId: String,
    val credentialTypes: List<String>,
)
