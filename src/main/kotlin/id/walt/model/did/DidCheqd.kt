package id.walt.model.did

import com.beust.klaxon.Json
import com.fasterxml.jackson.annotation.JsonProperty
import id.walt.model.Did

import id.walt.model.ServiceEndpoint
import id.walt.model.VerificationMethod
import kotlinx.serialization.SerialName

class DidCheqd(
    context: List<String>,
    id: String, // did:cheqd:mainnet:zF7rhDBfUt9d1gJPjx7s1JXfUY7oVWkY
    val service: List<Service>? = null,
    @SerialName("authentication") @JsonProperty("authentication") @Json(name = "authentication") val cheqdAuthentication: List<String>? = null,
    @SerialName("_authentication") @JsonProperty("_authentication") @Json(name = "_authentication") override var authentication: List<VerificationMethod>? = null,
    verificationMethod: List<VerificationMethod>? = null,
    assertionMethod: List<VerificationMethod>? = null,
    capabilityDelegation: List<VerificationMethod>? = null,
    capabilityInvocation: List<VerificationMethod>? = null,
    keyAgreement: List<VerificationMethod>? = null,
    serviceEndpoint: List<ServiceEndpoint>? = null
) : Did(
    context = context,
    id = id,
    verificationMethod = verificationMethod,
    authentication = emptyList(),
    assertionMethod = assertionMethod,
    capabilityDelegation = capabilityDelegation,
    capabilityInvocation = capabilityInvocation,
    keyAgreement = keyAgreement,
    serviceEndpoint = serviceEndpoint
) {
    data class Service(
        var id: String?, // did:cheqd:mainnet:zF7rhDBfUt9d1gJPjx7s1JXfUY7oVWkY#website
        var serviceEndpoint: String?, // https://www.cheqd.io
        var type: String? // LinkedDomains
    )

    constructor(context: String,
                id: String, // did:cheqd:mainnet:zF7rhDBfUt9d1gJPjx7s1JXfUY7oVWkY
                service: List<Service>? = null,
                authentication: List<String>? = null,
                verificationMethod: List<VerificationMethod>? = null,
                assertionMethod: List<VerificationMethod>? = null,
                capabilityDelegation: List<VerificationMethod>? = null,
                capabilityInvocation: List<VerificationMethod>? = null,
                keyAgreement: List<VerificationMethod>? = null,
                serviceEndpoint: List<ServiceEndpoint>? = null
    ) : this(
        listOf(context),
        id,
        service,
        authentication,
        emptyList(),
        verificationMethod,
        assertionMethod,
        capabilityDelegation,
        capabilityInvocation,
        keyAgreement,
        serviceEndpoint
    )

    override fun toString(): String {
        return "DidCheqd(context=$context, id=$id, verificationMethod=$verificationMethod, cheqdAuthentication=$cheqdAuthentication, authentication=$authentication, service=$service)"
    }


}
