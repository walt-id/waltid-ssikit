package id.walt.model

import com.beust.klaxon.Json
import id.walt.vclib.model.Proof

class DidEbsi(
    context: List<String>,
    id: String,
    verificationMethod: List<VerificationMethod>? = null,
    authentication: List<VerificationMethod>? = null,
    assertionMethod: List<VerificationMethod>? = null,
    capabilityDelegation: List<VerificationMethod>? = null,
    capabilityInvocation: List<VerificationMethod>? = null,
    keyAgreement: List<VerificationMethod>? = null,
    serviceEndpoint: List<ServiceEndpoint>? = null,
    @Json(serializeNull = false) var proof: Proof? = null
) : Did(
    context,
    id,
    verificationMethod,
    authentication,
    assertionMethod,
    capabilityDelegation,
    capabilityInvocation,
    keyAgreement,
    serviceEndpoint
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
        proof: Proof? = null
    ) : this(
        listOf(context),
        id,
        verificationMethod,
        authentication,
        assertionMethod,
        capabilityDelegation,
        capabilityInvocation,
        keyAgreement,
        serviceEndpoint,
        proof
    )
}
