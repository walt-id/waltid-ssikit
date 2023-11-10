package id.walt.credentials.jsonld

import com.apicatalog.jsonld.loader.DocumentLoader
import id.walt.servicematrix.ServiceProvider
import id.walt.servicematrix.ServiceRegistry
import id.walt.services.WaltIdService

abstract class JsonLdDocumentLoaderService: WaltIdService() {

    override val implementation: JsonLdDocumentLoaderService get() = serviceImplementation()

    abstract val documentLoader: DocumentLoader

    companion object : ServiceProvider {
        override fun getService() = ServiceRegistry.getService(JsonLdDocumentLoaderService::class)
        override fun defaultImplementation() = LocalJsonLdDocumentLoaderService()

    }
}
