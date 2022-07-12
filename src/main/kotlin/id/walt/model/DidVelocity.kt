package id.walt.model

class DidVelocity(
    context: List<String>,
    id: String,
    verificationMethod: List<VerificationMethod>? = null,
    assertionMethod: List<String>? = null,
    serviceEndpoint: List<ServiceEndpoint>? = null,
) : Did(
    context = context,
    id = id,
    verificationMethod = verificationMethod,
    assertionMethod = assertionMethod,
    serviceEndpoint = serviceEndpoint
) {
    constructor(
        context: String,
        id: String,
        verificationMethod: List<VerificationMethod>? = null,
        assertionMethod: List<String>? = null,
        serviceEndpoint: List<ServiceEndpoint>? = null
    ) : this(listOf(context), id, verificationMethod, assertionMethod, serviceEndpoint)
}