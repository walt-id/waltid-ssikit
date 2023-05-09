package id.walt.services.ecosystems.velocity.issuer

import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import io.ktor.client.statement.*

open class IssuerVelocityService : WaltIdService() {
    override val implementation get() = serviceImplementation<IssuerVelocityService>()

    open suspend fun initExchange(did: String): HttpResponse = implementation.initExchange(did)
    open suspend fun initDisclosure(exchangeId: String, holder: String, issuerDid: String): HttpResponse =
        implementation.initDisclosure(exchangeId, holder, issuerDid)
    open suspend fun generateOffers(
        exchangeId: String,
        issuerDid: String,
        credentialTypes: List<String>
    ): HttpResponse = implementation.generateOffers(exchangeId, issuerDid, credentialTypes)
    open suspend fun finalizeOffers(
        exchangeId: String,
        issuerDid: String,
        accepted: List<String>,
        rejected: List<String>
    ): HttpResponse =
        implementation.finalizeOffers(exchangeId, issuerDid, accepted, rejected)

    companion object : ServiceProvider {
        override fun getService() = object : IssuerVelocityService() {}
        override fun defaultImplementation() = WaltIdIssuerVelocityService()
    }
}
