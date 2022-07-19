package id.walt.services.velocitynetwork.did

import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.services.velocitynetwork.models.responses.CreateOrganizationResponse

open class DidVelocityService : WaltIdService() {
    override val implementation get() = serviceImplementation<DidVelocityService>()

    open fun registerOrganization(
        data: String,
        token: String,
        onResult: (
            did: String,
            didDoc: String,
            keys: List<CreateOrganizationResponse.Key>,
            authClients: List<CreateOrganizationResponse.AuthClient>,
        ) -> Unit
    ): Unit = implementation.registerOrganization(data, token, onResult)
    open fun validate(data: String): Boolean = implementation.validate(data)


    companion object : ServiceProvider {
        override fun getService() = object : DidVelocityService() {}
        override fun defaultImplementation() = WaltIdDidVelocityService()
    }
}