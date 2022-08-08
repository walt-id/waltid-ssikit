package id.walt.services.velocitynetwork.models.requests

import kotlinx.serialization.Serializable

@Serializable
data class CreateDisclosureRequest(
    val types: List<Type>,
    val vendorEndpoint: String,
    val vendorDisclosureId: String,
    val purpose: String,
    val duration: String,
    val termsUrl: String,
    val activationDate: String,
){
    @Serializable
    data class Type(
        val type: String
    )
}
