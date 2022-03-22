package id.walt.model

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class DidWeb(
    context: List<String>,
    id: String,
    verificationMethod: List<VerificationMethod>? = null,
    authentication: List<String>? = null,
    assertionMethod: List<String>? = null,
    capabilityDelegation: List<String>? = null,
    capabilityInvocation: List<String>? = null,
    keyAgreement: List<String>? = null,
    serviceEndpoint: List<ServiceEndpoint>? = null
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
                serviceEndpoint: List<ServiceEndpoint>? = null) : this(listOf(context), id, verificationMethod, authentication, assertionMethod, capabilityDelegation, capabilityInvocation, keyAgreement, serviceEndpoint)

    companion object {
      fun getDomain(didUrl: DidUrl) =
        didUrl.identifier.substringBefore(":").let { URLDecoder.decode(it, StandardCharsets.UTF_8) }
      fun getPath(didUrl: DidUrl) =
        didUrl.identifier.substringAfter(":", "").split(":").map { part -> URLDecoder.decode(part, StandardCharsets.UTF_8) }.joinToString("/")
      fun getDidDocUri(didUrl: DidUrl): URI {
        val path = getPath(didUrl)
        if (path.isEmpty()) {
          return URI.create("https://${getDomain(didUrl)}/.well-known/did.json")
        } else {
          return URI.create("https://${getDomain(didUrl)}/${path}/did.json")
        }
      }
    }
}
