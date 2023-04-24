package id.walt.signatory.revocation.statuslist2021

import id.walt.credentials.w3c.VerifiableCredential
import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService

open class StatusListCredentialStorageService : WaltIdService() {
    override val implementation get() = serviceImplementation<StatusListCredentialStorageService>()
    open fun fetch(id: String): VerifiableCredential? = implementation.fetch(id)

    companion object : ServiceProvider {
        override fun getService() = object : StatusListCredentialStorageService() {}
        override fun defaultImplementation() = WaltIdStatusListCredentialStorageService()
    }
}


class WaltIdStatusListCredentialStorageService: StatusListCredentialStorageService() {
    private val path = "data/status-list-credentials"

    override fun fetch(id: String): VerifiableCredential? {
        return super.fetch(id)
    }
}
