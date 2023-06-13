package id.walt.services.did.composers.models

import com.nimbusds.jose.jwk.JWK
import id.walt.crypto.Key
import id.walt.model.DidUrl

open class DocumentComposerBaseParameter(
    open val didUrl: DidUrl
)

open class DocumentComposerJwkParameter(
    override val didUrl: DidUrl,
    open val jwk: JWK,
) : DocumentComposerBaseParameter(didUrl)

open class DocumentComposerKeyJwkParameter(
    override val didUrl: DidUrl,
    override val jwk: JWK,
    open val key: Key
) : DocumentComposerJwkParameter(didUrl, jwk)
