package id.walt.services.ecosystems.cheqd.models.job.request

import kotlinx.serialization.Serializable

@Serializable
data class JobDeactivateRequest(
    val did: String
)
