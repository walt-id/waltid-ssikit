package id.walt.services.ecosystems.essif.didebsi

import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.services.ecosystems.essif.EbsiEnvironment
import id.walt.services.ecosystems.essif.jsonrpc.JsonRpcParams

open class DidEbsiService : WaltIdService() {

    override val implementation get() = serviceImplementation<DidEbsiService>()

    open fun apiVersion(): String = implementation.apiVersion()

    open fun registerDid(did: String, ethKeyAlias: String): Unit = implementation.registerDid(did, ethKeyAlias)

    open fun buildUnsignedTransactionParams(did: String, ethKeyAlias: String? = null): List<JsonRpcParams> =
        implementation.buildUnsignedTransactionParams(did, ethKeyAlias)

    protected fun rpcUrl(): String = "${EbsiEnvironment.url()}/did-registry/${apiVersion()}/jsonrpc"

    companion object : ServiceProvider {
        override fun getService() = object : DidEbsiService() {}
        override fun defaultImplementation() = WaltIdDidEbsiV5Service()
    }
}
