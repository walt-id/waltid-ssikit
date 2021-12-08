package id.walt.services.essif.didebsi

import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.services.essif.jsonrpc.InsertDidDocumentParams

open class DidEbsiService : WaltIdService() {

    override val implementation get() = serviceImplementation<DidEbsiService>()

    open fun registerDid(did: String, ethKeyAlias: String): Unit = implementation.registerDid(did, ethKeyAlias)

    open fun buildUnsignedTransactionParams(did: String, ethKeyAlias: String? = null): List<InsertDidDocumentParams> =
        implementation.buildUnsignedTransactionParams(did, ethKeyAlias)

    companion object : ServiceProvider {
        override fun getService() = object : DidEbsiService() {}
    }
}
