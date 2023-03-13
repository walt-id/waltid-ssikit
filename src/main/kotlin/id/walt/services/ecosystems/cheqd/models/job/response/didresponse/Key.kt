package id.walt.services.ecosystems.cheqd.models.job.response.didresponse

import kotlinx.serialization.Serializable

@Serializable
data class Key(
    val publicKeyHex: String,
    val verificationMethodId: String
)
