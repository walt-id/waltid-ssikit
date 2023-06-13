package id.walt.services.did.composers

import id.walt.model.DID_CONTEXT_URL
import id.walt.model.VerificationMethod
import id.walt.model.did.DidEbsi
import id.walt.services.did.composers.models.DocumentComposerBaseParameter
import id.walt.services.did.composers.models.DocumentComposerKeyJwkParameter

class DidEbsiV1DocumentComposer : DidDocumentComposerBase<DidEbsi>() {
    override fun make(parameter: DocumentComposerBaseParameter): DidEbsi =
        (parameter as? DocumentComposerKeyJwkParameter)?.let {
            val kid = it.didUrl.did + "#" + it.key.keyId
            val verificationMethods = buildVerificationMethods(it.key, kid, it.didUrl.did, it.jwk)
            val keyRef = listOf(VerificationMethod.Reference(kid))
            DidEbsi(
                listOf(DID_CONTEXT_URL), // TODO Context not working "https://ebsi.org/ns/did/v1"
                it.didUrl.did, verificationMethods, keyRef, keyRef
            )
        } ?: throw IllegalArgumentException("Couldn't parse ebsi-v1 document composer parameter")
}
