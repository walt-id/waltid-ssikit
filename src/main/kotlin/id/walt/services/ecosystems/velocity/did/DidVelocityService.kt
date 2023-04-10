package id.walt.services.ecosystems.velocity.did

import id.walt.model.DidUrl
import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import io.ktor.client.statement.*

open class DidVelocityService : WaltIdService() {
    override val implementation get() = serviceImplementation<DidVelocityService>()

    open suspend fun resolveDid(didUrl: DidUrl): HttpResponse = implementation.resolveDid(didUrl)
    open suspend fun onboard(orgData: String): HttpResponse = implementation.onboard(orgData)

    companion object : ServiceProvider {
        override fun getService() = object : DidVelocityService() {}
        override fun defaultImplementation() = WaltIdDidVelocityService()
    }
}
