package id.walt.services.did.resolvers

import id.walt.model.DidUrl
import id.walt.model.did.DidKey
import id.walt.services.did.DidOptions
import id.walt.services.did.composers.DidDocumentComposer
import id.walt.services.did.composers.models.DocumentComposerBaseParameter

class DidKeyResolver(
    private val documentComposer: DidDocumentComposer<DidKey>,
) : DidResolverBase<DidKey>() {
    override fun resolve(didUrl: DidUrl, options: DidOptions?) = documentComposer.make(DocumentComposerBaseParameter(didUrl))
}
