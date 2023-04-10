package id.walt.services.ecosystems.velocity.tenant

import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import io.ktor.client.statement.*

open class TenantVelocityService: WaltIdService() {
    override val implementation get() = serviceImplementation<TenantVelocityService>()

    open suspend fun create(tenantData: String): HttpResponse = implementation.create(tenantData)
    open suspend fun addDisclosure(did: String, disclosureData: String): HttpResponse =
        implementation.addDisclosure(did, disclosureData)

    companion object : ServiceProvider {
        override fun getService() = object : TenantVelocityService() {}
        override fun defaultImplementation() = WaltIdTenantVelocityService()
    }
}
