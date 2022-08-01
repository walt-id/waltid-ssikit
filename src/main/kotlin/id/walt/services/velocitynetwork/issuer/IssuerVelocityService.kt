package id.walt.services.velocitynetwork.issuer

import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.services.velocitynetwork.models.responses.DisclosureResponse
import id.walt.services.velocitynetwork.models.responses.ExchangeResponse
import id.walt.services.velocitynetwork.models.responses.OfferResponse

open class IssuerVelocityService : WaltIdService() {
    override val implementation get() = serviceImplementation<IssuerVelocityService>()

    open suspend fun initExchange(issuerDid: String): ExchangeResponse = implementation.initExchange(issuerDid)
    open suspend fun initDisclosure(exchangeId: String, holder: String, issuerDid: String): DisclosureResponse =
        implementation.initDisclosure(exchangeId, holder, issuerDid)
    open suspend fun generateOffers(
        exchangeId: String,
        issuerDid: String,
        credentialTypes: List<String>
    ): List<OfferResponse> = implementation.generateOffers(exchangeId, issuerDid, credentialTypes)
    open suspend fun finalizeOffers(exchangeId: String, issuerDid: String, offers: List<String>): List<String> =
        implementation.finalizeOffers(exchangeId, issuerDid, offers)

    companion object : ServiceProvider {
        override fun getService() = object : IssuerVelocityService() {}
        override fun defaultImplementation() = WaltIdIssuerVelocityService()
    }
}