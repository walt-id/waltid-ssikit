package id.walt.services.did.composers

import id.walt.model.DID_CONTEXT_URL
import id.walt.model.VerificationMethod
import id.walt.model.did.DidWeb
import id.walt.services.did.composers.models.DocumentComposerBaseParameter
import id.walt.services.did.composers.models.DocumentComposerKeyJwkParameter

class DidWebDocumentComposer : DidDocumentComposerBase<DidWeb>() {
    override fun make(parameter: DocumentComposerBaseParameter): DidWeb = (parameter as? DocumentComposerKeyJwkParameter)?.let {
        val kid = it.didUrl.did + "#" + it.key.keyId
        val verificationMethods = buildVerificationMethods(it.key, kid, it.didUrl.did, it.jwk)
        val keyRef = listOf(VerificationMethod.Reference(kid))
        DidWeb(DID_CONTEXT_URL, it.didUrl.did, verificationMethods, keyRef, keyRef)
    } ?: throw IllegalArgumentException("Couldn't parse web document composer parameter")
}
