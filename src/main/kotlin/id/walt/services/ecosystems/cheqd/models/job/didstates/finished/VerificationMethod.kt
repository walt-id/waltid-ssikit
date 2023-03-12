package id.walt.services.ecosystems.cheqd.models.job.didstates.finished

import kotlinx.serialization.Serializable

@Serializable
data class VerificationMethod(
    val controller: String,
    val id: String,
    val publicKeyMultibase: String,
    val type: String
)
