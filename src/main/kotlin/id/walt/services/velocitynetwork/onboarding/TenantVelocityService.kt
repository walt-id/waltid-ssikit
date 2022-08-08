package id.walt.services.velocitynetwork.onboarding

import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService

open class TenantVelocityService: WaltIdService() {
    override val implementation get() = serviceImplementation<TenantVelocityService>()

    open suspend fun create(tenantData: String): String = implementation.create(tenantData)
    open suspend fun addDisclosure(did: String, disclosureData: String): String =
        implementation.addDisclosure(did, disclosureData)

    companion object : ServiceProvider {
        override fun getService() = object : TenantVelocityService() {}
        override fun defaultImplementation() = WaltIdTenantVelocityService()
    }
}