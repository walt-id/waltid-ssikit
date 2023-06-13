package id.walt.services.did.composers

import id.walt.model.Did
import id.walt.services.did.composers.models.DocumentComposerBaseParameter

interface DidDocumentComposer<T : Did> {
    fun make(parameter: DocumentComposerBaseParameter): T
}
