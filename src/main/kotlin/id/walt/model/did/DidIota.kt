package id.walt.model.did

import com.beust.klaxon.Json
import id.walt.credentials.w3c.W3CProof
import id.walt.model.Did
import id.walt.model.ServiceEndpoint
import id.walt.model.VerificationMethod

class DidIota(
    context: List<String>? = listOf("https://www.w3.org/ns/did/v1"),
    id: String,
    verificationMethod: List<VerificationMethod>? = null,
    authentication: List<VerificationMethod>? = null,
    assertionMethod: List<VerificationMethod>? = null,
    capabilityDelegation: List<VerificationMethod>? = null,
    capabilityInvocation: List<VerificationMethod>? = null,
    keyAgreement: List<VerificationMethod>? = null,
    serviceEndpoint: List<ServiceEndpoint>? = null,
    @Json(serializeNull = false) var proof: W3CProof? = null
) : Did(
    context = context,
    id = id,
    verificationMethod = verificationMethod,
    authentication = authentication,
    assertionMethod = assertionMethod,
    capabilityDelegation = capabilityDelegation,
    capabilityInvocation = capabilityInvocation,
    keyAgreement = keyAgreement,
    service = serviceEndpoint
) {
    constructor(
        context: String,
        id: String,
        verificationMethod: List<VerificationMethod>? = null,
        authentication: List<VerificationMethod>? = null,
        assertionMethod: List<VerificationMethod>? = null,
        capabilityDelegation: List<VerificationMethod>? = null,
        capabilityInvocation: List<VerificationMethod>? = null,
        keyAgreement: List<VerificationMethod>? = null,
        serviceEndpoint: List<ServiceEndpoint>? = null,
        proof: W3CProof? = null
    ) : this(
        context = listOf(context),
        id = id,
        verificationMethod = verificationMethod,
        authentication = authentication,
        assertionMethod = assertionMethod,
        capabilityDelegation = capabilityDelegation,
        capabilityInvocation = capabilityInvocation,
        keyAgreement = keyAgreement,
        serviceEndpoint = serviceEndpoint,
        proof = proof
    )
}
