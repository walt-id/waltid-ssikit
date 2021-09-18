package id.walt.services.vcstore

import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.vclib.model.VerifiableCredential

abstract class VcStoreService : WaltIdService() {
    override val implementation get() = serviceImplementation<VcStoreService>()

    companion object : ServiceProvider {
        override fun getService() = object : VcStoreService() {}
    }

    open fun getCredential(id: String, group: String = ""): VerifiableCredential = implementation.getCredential(id)
    open fun listCredentialIds(group: String = ""): List<String> = implementation.listCredentialIds()
    open fun listCredentials(group: String = ""): List<VerifiableCredential> = implementation.listCredentials()
    open fun storeCredential(alias: String, vc: VerifiableCredential, group: String = ""): Unit = implementation.storeCredential(alias, vc)
    open fun deleteCredential(alias: String, group: String = ""): Boolean = implementation.deleteCredential(alias)
}

