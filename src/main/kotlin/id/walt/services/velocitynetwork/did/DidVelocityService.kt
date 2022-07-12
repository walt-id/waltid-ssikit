package id.walt.services.velocitynetwork.did

import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService

open class DidVelocityService : WaltIdService() {
    override val implementation get() = serviceImplementation<DidVelocityService>()

    open fun createOrganization(data: String?): Unit = implementation.createOrganization(data)
    open fun validate(data: String): Boolean = implementation.validate(data)


    companion object : ServiceProvider {
        override fun getService() = object : DidVelocityService() {}
        override fun defaultImplementation() = WaltIdDidVelocityService()
    }
}