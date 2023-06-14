package id.walt.services.did.resolvers

import id.walt.model.Did
import id.walt.model.DidUrl
import id.walt.services.did.DidOptions
import id.walt.services.did.composers.DidDocumentComposer
import id.walt.services.did.composers.models.DocumentComposerBaseParameter

class DidJwkResolver(
    private val documentComposer: DidDocumentComposer<Did>,
) : DidResolverBase<Did>() {
    override fun resolve(didUrl: DidUrl, options: DidOptions?): Did =
        documentComposer.make(DocumentComposerBaseParameter(didUrl))
}
