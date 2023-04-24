package id.walt.signatory.revocation.statuslist2021

import id.walt.credentials.w3c.VerifiableCredential
import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService

open class StatusListCredentialPublishingService : WaltIdService() {
    override val implementation get() = serviceImplementation<StatusListCredentialPublishingService>()

    open fun publish(vc: VerifiableCredential): String = implementation.publish(vc)

    companion object : ServiceProvider {
        override fun getService() = object : StatusListCredentialPublishingService() {}
        override fun defaultImplementation() = WaltIdStatusListCredentialPublishingService()
    }
}


class WaltIdStatusListCredentialPublishingService: StatusListCredentialPublishingService() {
    private val path = "data/status-list-credentials"

    override fun publish(vc: VerifiableCredential): String = super.publish(vc)
}
