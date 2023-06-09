package id.walt.services.did.factories

import id.walt.crypto.Key
import id.walt.model.DID_CONTEXT_URL
import id.walt.model.Did
import id.walt.model.DidUrl
import id.walt.model.VerificationMethod
import id.walt.model.did.DidWeb
import id.walt.services.did.DidOptions
import id.walt.services.did.DidWebCreateOptions
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class DidWebFactory: DidFactoryBase() {
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
        val didUrlStr = DidUrl("web", "$domain$path").did
        val kid = didUrlStr + "#" + key.keyId
        val verificationMethods = buildVerificationMethods(key, kid, didUrlStr)
        val keyRef = listOf(VerificationMethod.Reference(kid))
        DidWeb(DID_CONTEXT_URL, didUrlStr, verificationMethods, keyRef, keyRef)
    }
}
