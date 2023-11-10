package id.walt.credentials.jsonld

import com.apicatalog.jsonld.loader.DocumentLoader
import info.weboftrust.ldsignatures.jsonld.LDSecurityContexts

class RemoteJsonLdDocumentLoaderService : JsonLdDocumentLoaderService() {
    override val documentLoader: DocumentLoader
        get() = LDSecurityContexts.DOCUMENT_LOADER
}
