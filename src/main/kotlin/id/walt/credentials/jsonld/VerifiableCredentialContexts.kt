package id.walt.credentials.jsonld

import com.apicatalog.jsonld.document.JsonDocument
import com.apicatalog.jsonld.http.media.MediaType
import com.apicatalog.jsonld.loader.DocumentLoader
import foundation.identity.jsonld.ConfigurableDocumentLoader
import info.weboftrust.ldsignatures.jsonld.LDSecurityContexts
import java.net.URI
import java.util.*

object VerifiableCredentialContexts {
    val DOCUMENT_LOADER: DocumentLoader by lazy { ConfigurableDocumentLoader(CONTEXTS) }
    private val CONTEXTS: Map<URI, JsonDocument> by lazy {
        val map = LDSecurityContexts.CONTEXTS
        runCatching { loadContextFiles() }.onSuccess {
            map.putAll(it)
        }
        for ((key, value) in map) {
            value.documentUrl = key
        }
        map
    }
    private val JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1 = URI.create("https://www.w3.org/2018/credentials/v1")

    private fun loadContextFiles() = mapOf(
        JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1 to JsonDocument.of(
            MediaType.JSON_LD, Objects.requireNonNull(
                VerifiableCredentialContexts::class.java.classLoader.getResourceAsStream("credentials-v1.jsonld")
            )
        )
    )
}
