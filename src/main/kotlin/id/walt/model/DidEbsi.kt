package id.walt.model

import com.beust.klaxon.Json
import id.walt.vclib.model.Proof

class DidEbsi(
    context: List<String>,
    id: String,
    verificationMethod: List<VerificationMethod>? = null,
    authentication: List<String>? = null,
    assertionMethod: List<String>? = null,
    capabilityDelegation: List<String>? = null,
    capabilityInvocation: List<String>? = null,
    keyAgreement: List<String>? = null,
    serviceEndpoint: List<VerificationMethod>? = null,
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
    constructor(context: String,
                id: String,
                verificationMethod: List<VerificationMethod>? = null,
                authentication: List<String>? = null,
                assertionMethod: List<String>? = null,
                capabilityDelegation: List<String>? = null,
                capabilityInvocation: List<String>? = null,
                keyAgreement: List<String>? = null,
                serviceEndpoint: List<VerificationMethod>? = null,
                proof: Proof? = null) : this(listOf(context), id, verificationMethod, authentication, assertionMethod, capabilityDelegation, capabilityInvocation, keyAgreement, serviceEndpoint, proof)
}