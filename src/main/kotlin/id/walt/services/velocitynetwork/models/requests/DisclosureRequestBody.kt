package id.walt.services.velocitynetwork.models.requests

import kotlinx.serialization.Serializable

@Serializable
data class DisclosureRequestBody(
    val exchangeId: String,
    val presentation: String,
)
