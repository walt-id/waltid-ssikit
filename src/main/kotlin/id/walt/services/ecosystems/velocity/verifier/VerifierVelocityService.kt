package id.walt.services.ecosystems.velocity.verifier

import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import io.ktor.client.statement.*

open class VerifierVelocityService : WaltIdService() {

    companion object : ServiceProvider {
        override fun getService() = object : VerifierVelocityService() {}
        override fun defaultImplementation() = WaltIdVerifierVelocityService()
    }

    override val implementation get() = serviceImplementation<VerifierVelocityService>()

    open suspend fun check(issuerDid: String, credential: String): HttpResponse = implementation.check(issuerDid, credential)
}
