package id.walt.model

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class DidWeb(
    context: List<String>,
    id: String,
    verificationMethod: List<VerificationMethod>? = null,
    authentication: List<VerificationMethod>? = null,
    assertionMethod: List<VerificationMethod>? = null,
    capabilityDelegation: List<VerificationMethod>? = null,
    capabilityInvocation: List<VerificationMethod>? = null,
    keyAgreement: List<VerificationMethod>? = null,
    serviceEndpoint: List<ServiceEndpoint>? = null
) : Did(
    context = context,
    id = id,
    verificationMethod = verificationMethod,
    authentication = authentication,
    assertionMethod = assertionMethod,
    capabilityDelegation = capabilityDelegation,
    capabilityInvocation = capabilityInvocation,
    keyAgreement = keyAgreement,
    serviceEndpoint = serviceEndpoint
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
        serviceEndpoint: List<ServiceEndpoint>? = null
    ) : this(
        context = listOf(context),
        id = id,
        verificationMethod = verificationMethod,
        authentication = authentication,
        assertionMethod = assertionMethod,
        capabilityDelegation = capabilityDelegation,
        capabilityInvocation = capabilityInvocation,
        keyAgreement = keyAgreement,
        serviceEndpoint = serviceEndpoint
    )

    companion object {
        fun getDomain(didUrl: DidUrl): String =
            didUrl.identifier.substringBefore(":").let { URLDecoder.decode(it, StandardCharsets.UTF_8) }

        fun getPath(didUrl: DidUrl) =
            didUrl.identifier.substringAfter(":", "").split(":")
                .joinToString("/") { part -> URLDecoder.decode(part, StandardCharsets.UTF_8) }

        fun getDidDocUri(didUrl: DidUrl): URI =
            getPath(didUrl).let { path ->
                return when {
                    path.isEmpty() -> URI.create("https://${getDomain(didUrl)}/.well-known/did.json")
                    else -> URI.create("https://${getDomain(didUrl)}/${path}/did.json")
                }
            }
    }
}
