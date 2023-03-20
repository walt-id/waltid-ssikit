package id.walt.services.ecosystems.cheqd.models.job.response

import id.walt.services.ecosystems.cheqd.models.job.didstates.DidState
import kotlinx.serialization.Serializable

@Serializable
data class JobActionResponse(
    val didState: DidState,
    val jobId: String
)
