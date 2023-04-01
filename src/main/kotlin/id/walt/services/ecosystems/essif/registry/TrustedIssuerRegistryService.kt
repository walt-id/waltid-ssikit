package id.walt.services.ecosystems.essif.registry

import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService

open class TrustedIssuerRegistryService : WaltIdService() {
    override val implementation get() = serviceImplementation<TrustedIssuerRegistryService>()

    open fun insertIssuer(): String = implementation.insertIssuer()

    companion object : ServiceProvider {
        override fun getService() = object : TrustedIssuerRegistryService() {}
        override fun defaultImplementation() = WaltidTrustedIssuerRegistryService()
    }
}
