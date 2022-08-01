package id.walt.services.velocitynetwork.issuer

import id.walt.services.WaltIdServices
import id.walt.services.velocitynetwork.VelocityNetwork
import id.walt.services.velocitynetwork.models.requests.*
import id.walt.services.velocitynetwork.models.responses.DisclosureResponse
import id.walt.services.velocitynetwork.models.responses.ExchangeResponse
import id.walt.services.velocitynetwork.models.responses.OfferResponse
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import mu.KotlinLogging

class WaltIdIssuerVelocityService : IssuerVelocityService() {
    companion object {
        const val exchangePath = "/api/holder/v0.6/org/%s/exchange"
        const val disclosurePath = "/api/holder/v0.6/org/%s/identify"
        const val offersPath = "/api/holder/v0.6/org/%s/issue/credential-offers"
        const val finalizePath = "/api/holder/v0.6/org/%s/issue/finalize-offers"
    }

    private val log = KotlinLogging.logger {}

    override suspend fun initExchange(issuerDid: String) =
        WaltIdServices.httpNoAuth.post(VelocityNetwork.agentUrl + exchangePath.format(issuerDid)) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(ExchangeRequestBody(ExchangeType.ISSUING.name))
        }.body<ExchangeResponse>()

    override suspend fun initDisclosure(exchangeId: String, holder: String, issuerDid: String) =
        WaltIdServices.httpNoAuth.post(VelocityNetwork.agentUrl + disclosurePath.format(issuerDid)) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(DisclosureRequestBody(exchangeId, holder))
        }.body<DisclosureResponse>()

    override suspend fun generateOffers(exchangeId: String, issuerDid: String, credentialTypes: List<String>) =
        WaltIdServices.httpWithAuth.post(VelocityNetwork.agentUrl + offersPath.format(issuerDid)) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(GetOffersRequestBody(exchangeId, credentialTypes))
        }.body<List<OfferResponse>>()

    override suspend fun finalizeOffers(exchangeId: String, issuerDid: String, offers: List<String>) =
        WaltIdServices.httpWithAuth.post(VelocityNetwork.agentUrl + finalizePath.format(issuerDid)) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(FinalizeOfferRequestBody(exchangeId, offers, emptyList()))
        }.body<List<String>>()
}