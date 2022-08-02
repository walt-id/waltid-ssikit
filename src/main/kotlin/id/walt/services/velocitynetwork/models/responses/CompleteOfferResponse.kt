package id.walt.services.velocitynetwork.models.responses

import kotlinx.serialization.Serializable

@Serializable
data class CompleteOfferResponse(
    val offerIds: List<String>,
)
