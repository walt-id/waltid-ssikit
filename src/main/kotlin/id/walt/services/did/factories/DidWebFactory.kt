package id.walt.services.did.factories

import id.walt.crypto.Key
import id.walt.model.Did
import id.walt.model.DidUrl
import id.walt.model.did.DidWeb
import id.walt.services.did.DidOptions
import id.walt.services.did.DidWebCreateOptions
import id.walt.services.did.composers.DidDocumentComposer
import id.walt.services.did.composers.models.DocumentComposerKeyJwkParameter
import id.walt.services.key.KeyService
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class DidWebFactory(
    private val keyService: KeyService,
    private val documentComposer: DidDocumentComposer<DidWeb>,
) : DidFactory {
    override fun create(key: Key, options: DidOptions?): Did = let {
        (options as? DidWebCreateOptions) ?: throw Exception("DidWebOptions are mandatory")
        if (options.domain.isNullOrEmpty()) throw IllegalArgumentException("Missing 'domain' parameter for creating did:web")
        val domain = URLEncoder.encode(options.domain, StandardCharsets.UTF_8)
        val path = when {
            options.path.isNullOrEmpty() -> ""
            else -> ":${
                options.path.split("/").joinToString(":") { part -> URLEncoder.encode(part, StandardCharsets.UTF_8) }
            }"
        }
        documentComposer.make(
            DocumentComposerKeyJwkParameter(
                DidUrl("web", "$domain$path"), keyService.toJwk(key.keyId.id), key
            )
        )
    }
}
