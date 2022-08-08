package id.walt.services.velocitynetwork.models.responses

data class CreateDisclosureResponse(
    val id: String,
    val description: String,
    val types: List<String>,
    val purpose: String,
    val duration: String,
    val vendorEndpoint: String,
    val termsUrl: String,
    val vendorDisclosureId: String,
    val activationDate: String,
    val createdAt: String,
    val updatedAt: String,
)
