package id.walt.services.velocitynetwork.models.requests

import kotlinx.serialization.Serializable

@Serializable
data class FinalizeOfferRequestBody(
    val exchangeId: String,
    val approvedOfferIds: List<String>,
    val rejectedOfferIds: List<String>
)