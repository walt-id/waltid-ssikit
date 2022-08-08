package id.walt.services.velocitynetwork.models.responses

import id.walt.services.velocitynetwork.models.CredentialCheckType
import id.walt.services.velocitynetwork.models.CredentialCheckValue
import kotlinx.serialization.Serializable

@Serializable
data class InspectionResult(
    val credentials: List<Credential>,
){
    @Serializable
    data class Credential(
        val checks: Map<CredentialCheckType, CredentialCheckValue>
    )
}