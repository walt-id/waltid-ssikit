package id.walt.services.velocitynetwork.models.responses

import kotlinx.serialization.Serializable

@Serializable
data class InspectionResult(
    val credentialId: String, //did
    val credentialChecks: String
)