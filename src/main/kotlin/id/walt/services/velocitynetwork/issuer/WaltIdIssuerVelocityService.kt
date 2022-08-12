package id.walt.services.velocitynetwork.issuer

import com.beust.klaxon.Klaxon
import id.walt.services.WaltIdServices
import id.walt.services.velocitynetwork.VelocityClient
import id.walt.services.velocitynetwork.models.ExchangeType
import id.walt.services.velocitynetwork.models.requests.*
import id.walt.services.velocitynetwork.models.responses.CompleteOfferResponse
import id.walt.services.velocitynetwork.models.responses.DisclosureResponse
import id.walt.services.velocitynetwork.models.responses.ExchangeResponse
import id.walt.services.velocitynetwork.models.responses.OfferResponse
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import mu.KotlinLogging

class WaltIdIssuerVelocityService : IssuerVelocityService() {
    companion object {
        const val holderExchangePath = "/api/holder/v0.6/org/%s/exchange"
        const val disclosurePath = "/api/holder/v0.6/org/%s/identify"
        const val offersPath = "/api/holder/v0.6/org/%s/issue/credential-offers"
        const val finalizePath = "/api/holder/v0.6/org/%s/issue/finalize-offers"

        const val exchangePath = "/operator-api/v0.8/tenants/%s/exchanges"
        const val offerPath = "/operator-api/v0.8/tenants/%s/exchanges/%s/offers"
        const val complete = "/operator-api/v0.8/tenants/%s/exchanges/%s/offers/complete"
        const val claim = "/operator-api/v0.8/tenants/%s/exchanges/%s/qrcode.uri"
    }

    private val log = KotlinLogging.logger {}

    override suspend fun initExchange(issuerDid: String) =
        WaltIdServices.httpWithAuth.post(VelocityClient.config.agentEndpoint + exchangePath.format(issuerDid)) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(ExchangeRequestBody(ExchangeType.ISSUING.name))
        }.body<ExchangeResponse>()

    override suspend fun addOffer(issuerDid: String, exchangeId: String, credential: String) =
        WaltIdServices.httpWithAuth.post(VelocityClient.config.agentEndpoint + offerPath.format(issuerDid, exchangeId)) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(Klaxon().parse<CredentialOffer>(credential))
        }.body<OfferResponse>()

    override suspend fun completeOffer(issuerDid: String, exchangeId: String) =
        WaltIdServices.httpWithAuth.post(VelocityClient.config.agentEndpoint + complete.format(issuerDid, exchangeId))
            .body<CompleteOfferResponse>()

    override suspend fun claimOffer(issuerDid: String, exchangeId: String) =
        WaltIdServices.httpWithAuth.get(VelocityClient.config.agentEndpoint + claim.format(issuerDid, exchangeId))
            .bodyAsText()



    override suspend fun initDisclosure(exchangeId: String, holder: String, issuerDid: String) =
        WaltIdServices.httpNoAuth.post(VelocityClient.config.agentEndpoint + disclosurePath.format(issuerDid)) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(DisclosureRequestBody(exchangeId, holder))
        }.body<DisclosureResponse>()

    override suspend fun generateOffers(exchangeId: String, issuerDid: String, credentialTypes: List<String>) =
        WaltIdServices.httpWithAuth.post(VelocityClient.config.agentEndpoint + offersPath.format(issuerDid)) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(GetOffersRequestBody(exchangeId, credentialTypes))
        }.body<List<OfferResponse>>()

    override suspend fun finalizeOffers(exchangeId: String, issuerDid: String, accepted: List<String>, rejected: List<String>) =
        WaltIdServices.httpWithAuth.post(VelocityClient.config.agentEndpoint + finalizePath.format(issuerDid)) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(FinalizeOfferRequestBody(exchangeId, accepted, rejected))
        }.body<List<String>>()
}