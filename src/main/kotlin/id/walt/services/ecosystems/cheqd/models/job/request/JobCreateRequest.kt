package id.walt.services.ecosystems.cheqd.models.job.request

import id.walt.services.ecosystems.cheqd.models.job.response.didresponse.DidDocObject
import kotlinx.serialization.Serializable

@Serializable
data class JobCreateRequest(
    val didDocument: DidDocObject
)
