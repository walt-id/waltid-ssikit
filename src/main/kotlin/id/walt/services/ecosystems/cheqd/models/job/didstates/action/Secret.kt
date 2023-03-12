package id.walt.services.ecosystems.cheqd.models.job.didstates.action

import kotlinx.serialization.Serializable

@Serializable
data class Secret(
    val signingResponse: List<String>
)
