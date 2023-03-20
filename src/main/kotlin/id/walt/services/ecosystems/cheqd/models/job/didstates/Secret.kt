package id.walt.services.ecosystems.cheqd.models.job.didstates

import kotlinx.serialization.Serializable

@Serializable
data class Secret(
    val signingResponse: List<SigningResponse>
)
