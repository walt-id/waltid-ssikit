package id.walt.services.vcstore

import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.vclib.model.VerifiableCredential

abstract class VcStoreService : WaltIdService() {
    override val implementation get() = serviceImplementation<VcStoreService>()

    companion object : ServiceProvider {
        override fun getService() = object : VcStoreService() {}
    }

    open fun getCredential(id: String): VerifiableCredential = implementation.getCredential(id)
    open fun listCredentialIds(): List<String> = implementation.listCredentialIds()
    open fun listCredentials(): List<VerifiableCredential> = implementation.listCredentials()
    open fun storeCredential(alias: String, vc: VerifiableCredential): Unit = implementation.storeCredential(alias, vc)
    open fun deleteCredential(alias: String): Boolean = implementation.deleteCredential(alias)
}

