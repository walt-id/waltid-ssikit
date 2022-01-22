package id.walt.services.vcstore

import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.vclib.model.VerifiableCredential

abstract class VcStoreService : WaltIdService() {
    override val implementation get() = serviceImplementation<VcStoreService>()

    open fun getCredential(id: String, group: String = ""): VerifiableCredential? = implementation.getCredential(id, group)
    open fun listCredentialIds(group: String = ""): List<String> = implementation.listCredentialIds(group)
    open fun listCredentials(group: String = ""): List<VerifiableCredential> = implementation.listCredentials(group)
    open fun storeCredential(alias: String, vc: VerifiableCredential, group: String = ""): Unit =
        implementation.storeCredential(alias, vc, group)

    open fun deleteCredential(alias: String, group: String = ""): Boolean = implementation.deleteCredential(alias, group)

    companion object : ServiceProvider {
        override fun getService() = object : VcStoreService() {}
        override fun defaultImplementation() = FileSystemVcStoreService()
    }
}

