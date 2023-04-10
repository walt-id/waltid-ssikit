package id.walt.services.ecosystems.velocity.onboarding

import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import io.ktor.client.statement.*

open class OfferVelocityService: WaltIdService() {
    override val implementation get() = serviceImplementation<OfferVelocityService>()

    open suspend fun initExchange(issuerDid: String, disclosureId: String, idMatcher: List<String>): HttpResponse =
        implementation.initExchange(issuerDid, disclosureId, idMatcher)
    open suspend fun addOffer(issuerDid: String, exchangeId: String, offer: String): HttpResponse =
        implementation.addOffer(issuerDid, exchangeId, offer)
    open suspend fun completeOffer(issuerDid: String, exchangeId: String): HttpResponse =
        implementation.completeOffer(issuerDid, exchangeId)
    open suspend fun claimOffer(issuerDid: String, exchangeId: String): HttpResponse =
        implementation.claimOffer(issuerDid, exchangeId)

    companion object : ServiceProvider {
        override fun getService() = object : OfferVelocityService() {}
        override fun defaultImplementation() = WaltIdOfferVelocityService()
    }
}
