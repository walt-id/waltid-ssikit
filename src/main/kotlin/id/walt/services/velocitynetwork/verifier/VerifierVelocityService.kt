package id.walt.services.velocitynetwork.verifier

import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService

open class VerifierVelocityService : WaltIdService() {

    companion object : ServiceProvider {
        override fun getService() = object : VerifierVelocityService() {}
        override fun defaultImplementation() = WaltIdVerifierVelocityService()
    }

    override val implementation get() = serviceImplementation<VerifierVelocityService>()

    open suspend fun check(issuerDid: String, id: String, credential: String): String = implementation.check(issuerDid, id, credential)
}