package id.walt.model.did

import id.walt.model.Did
import id.walt.model.ServiceEndpoint
import id.walt.model.VerificationMethod

class DidCheqd(
    context: List<String>? = listOf("https://w3id.org/did-resolution/v1"),
    id: String, // did:cheqd:mainnet:zF7rhDBfUt9d1gJPjx7s1JXfUY7oVWkY
    authentication: List<VerificationMethod>? = null,
    verificationMethod: List<VerificationMethod>? = null,
    assertionMethod: List<VerificationMethod>? = null,
    capabilityDelegation: List<VerificationMethod>? = null,
    capabilityInvocation: List<VerificationMethod>? = null,
    keyAgreement: List<VerificationMethod>? = null,
    service: List<ServiceEndpoint>? = null
) : Did(
    context = context,
    id = id,
    verificationMethod = verificationMethod,
    authentication = authentication,
    assertionMethod = assertionMethod,
    capabilityDelegation = capabilityDelegation,
    capabilityInvocation = capabilityInvocation,
    keyAgreement = keyAgreement,
    service = service
) {

    constructor(
        context: String,
        id: String, // did:cheqd:mainnet:zF7rhDBfUt9d1gJPjx7s1JXfUY7oVWkY
        authentication: List<VerificationMethod>? = null,
        verificationMethod: List<VerificationMethod>? = null,
        assertionMethod: List<VerificationMethod>? = null,
        capabilityDelegation: List<VerificationMethod>? = null,
        capabilityInvocation: List<VerificationMethod>? = null,
        keyAgreement: List<VerificationMethod>? = null,
        service: List<ServiceEndpoint>? = null
    ) : this(
        context = listOf(context),
        id = id,
        authentication = authentication,
        verificationMethod = verificationMethod,
        assertionMethod = assertionMethod,
        capabilityDelegation = capabilityDelegation,
        capabilityInvocation = capabilityInvocation,
        keyAgreement = keyAgreement,
        service = service
    )

    override fun toString(): String {
        return "DidCheqd(context=$context, id=$id, verificationMethod=$verificationMethod, authentication=$authentication, service=$service)"
    }


}
