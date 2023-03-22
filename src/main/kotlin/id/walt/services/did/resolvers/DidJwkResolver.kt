package id.walt.services.did.resolvers

import com.beust.klaxon.Klaxon
import com.nimbusds.jose.util.Base64URL
import id.walt.model.Did
import id.walt.model.DidUrl
import id.walt.model.Jwk
import id.walt.model.VerificationMethod
import id.walt.services.did.DidOptions

class DidJwkResolver : DidResolverBase<Did>() {
    override fun resolve(didUrl: DidUrl, options: DidOptions?): Did = Did(
        context = listOf("https://www.w3.org/ns/did/v1", "https://w3id.org/security/suites/jws-2020/v1"),
        id = didUrl.did,
        verificationMethod = listOf(
            VerificationMethod(
                id = "${didUrl.did}#0",
                type = "JsonWebKey2020",
                controller = didUrl.did,
                publicKeyJwk = Klaxon().parse<Jwk>(Base64URL.from(didUrl.identifier).decodeToString())
            )
        ),
        assertionMethod = listOf(VerificationMethod.Reference("${didUrl.did}#0")),
        authentication = listOf(VerificationMethod.Reference("${didUrl.did}#0")),
        capabilityInvocation = listOf(VerificationMethod.Reference("${didUrl.did}#0")),
        capabilityDelegation = listOf(VerificationMethod.Reference("${didUrl.did}#0")),
        keyAgreement = listOf(VerificationMethod.Reference("${didUrl.did}#0"))
    )
}
