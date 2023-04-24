package id.walt.signatory.revocation.statuslist2021

import id.walt.credentials.w3c.VerifiableCredential
import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService

open class StatusListCredentialStorageService : WaltIdService() {
    override val implementation get() = serviceImplementation<StatusListCredentialStorageService>()

    open val publicUrl = ""
    open fun fetch(id: String): VerifiableCredential? = implementation.fetch(id)
    open fun store(vc: VerifiableCredential): String = implementation.store(vc)

    companion object : ServiceProvider {
        override fun getService() = object : StatusListCredentialStorageService() {}
        override fun defaultImplementation() = WaltIdStatusListCredentialStorageService()
    }
}


class WaltIdStatusListCredentialStorageService : StatusListCredentialStorageService() {
    private val rootPath = "data/status-list-credentials"

    override val publicUrl: String get() = "https://raw.githubusercontent.com/walt-id/waltid-ssikit/main/src/test/resources/credential-status/status-list-credential.json"
    override fun fetch(id: String): VerifiableCredential? {
        return super.fetch(id)
    }
    override fun store(vc: VerifiableCredential): String = super.store(vc)
}
