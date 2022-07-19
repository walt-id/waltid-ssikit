package id.walt.services.velocitynetwork.models.requests

import kotlinx.serialization.Serializable

@Serializable
data class ExchangeRequestBody(
    val type: String
)
