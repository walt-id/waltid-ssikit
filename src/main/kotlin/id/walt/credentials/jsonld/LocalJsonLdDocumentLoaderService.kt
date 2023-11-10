package id.walt.credentials.jsonld

import com.apicatalog.jsonld.loader.DocumentLoader

class LocalJsonLdDocumentLoaderService : JsonLdDocumentLoaderService() {
    override val documentLoader: DocumentLoader
        get() = VerifiableCredentialContexts.DOCUMENT_LOADER
}
