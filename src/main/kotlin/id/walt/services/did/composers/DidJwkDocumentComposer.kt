package id.walt.services.did.composers

import com.beust.klaxon.Klaxon
import com.nimbusds.jose.util.Base64URL
import id.walt.model.Did
import id.walt.model.Jwk
import id.walt.model.VerificationMethod
import id.walt.services.did.composers.models.DocumentComposerBaseParameter

class DidJwkDocumentComposer : DidDocumentComposerBase<Did>() {
    override fun make(parameter: DocumentComposerBaseParameter): Did = Did(
        context = listOf("https://www.w3.org/ns/did/v1", "https://w3id.org/security/suites/jws-2020/v1"),
        id = parameter.didUrl.did,
        verificationMethod = listOf(
            VerificationMethod(
                id = "${parameter.didUrl.did}#0",
                type = "JsonWebKey2020",
                controller = parameter.didUrl.did,
                publicKeyJwk = Klaxon().parse<Jwk>(Base64URL.from(parameter.didUrl.identifier).decodeToString())
            )
        ),
        assertionMethod = listOf(VerificationMethod.Reference("${parameter.didUrl.did}#0")),
        authentication = listOf(VerificationMethod.Reference("${parameter.didUrl.did}#0")),
        capabilityInvocation = listOf(VerificationMethod.Reference("${parameter.didUrl.did}#0")),
        capabilityDelegation = listOf(VerificationMethod.Reference("${parameter.didUrl.did}#0")),
        keyAgreement = listOf(VerificationMethod.Reference("${parameter.didUrl.did}#0"))
    )
}
