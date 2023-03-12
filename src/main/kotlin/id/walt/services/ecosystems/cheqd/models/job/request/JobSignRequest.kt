package id.walt.services.ecosystems.cheqd.models.job.request

import id.walt.services.ecosystems.cheqd.models.job.didstates.Secret
import kotlinx.serialization.Serializable

@Serializable
data class JobSignRequest(
    val jobId: String,
    val secret: Secret
)
