package id.walt.services.did.factories

import com.nimbusds.jose.util.Base64URL
import id.walt.crypto.Key
import id.walt.model.Did
import id.walt.model.DidUrl
import id.walt.services.did.DidOptions
import id.walt.services.did.composers.DidJwkDocumentComposer
import id.walt.services.did.composers.models.DocumentComposerBaseParameter
import id.walt.services.key.KeyService

class DidJwkFactory(
    private val keyService: KeyService,
    private val documentComposer: DidJwkDocumentComposer,
) : DidFactory {
    override fun create(key: Key, options: DidOptions?): Did =
        documentComposer.make(DocumentComposerBaseParameter(DidUrl.from("did:jwk:${Base64URL.encode(keyService.toJwk(key.keyId.id).toString())}")))
}
