package id.walt.services.ecosystems.cheqd.models.job.response.didresponse

import kotlinx.serialization.Serializable

@Serializable
data class DidGetResponse(
    val didDoc: DidDocObject,
    val key: Key
)
