package id.walt.services.ecosystems.velocity.issuer

import id.walt.services.WaltIdServices
import id.walt.services.ecosystems.velocity.VelocityClient
import id.walt.services.ecosystems.velocity.models.*
import io.ktor.client.request.*
import io.ktor.http.*

class WaltIdIssuerVelocityService : IssuerVelocityService() {
    companion object {
        const val exchangePath = "/api/holder/%s/org/%s/exchange"
        const val disclosurePath = "/api/holder/%s/org/%s/identify"
        const val offersPath = "/api/holder/%s/org/%s/issue/credential-offers"
        const val finalizePath = "/api/holder/%s/org/%s/issue/finalize-offers"
    }

    override suspend fun initExchange(did: String) =
        WaltIdServices.httpNoAuth.post(
            VelocityClient.config.agentEndpoint + exchangePath.format(
                VelocityClient.config.registrarApiVersion,
                did
            )
        ) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(HolderExchangeRequestBody(ExchangeType.ISSUING.name))
        }

    override suspend fun initDisclosure(exchangeId: String, holder: String, issuerDid: String) =
        WaltIdServices.httpNoAuth.post(
            VelocityClient.config.agentEndpoint + disclosurePath.format(
                VelocityClient.config.registrarApiVersion,
                issuerDid
            )
        ) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(DisclosureRequestBody(exchangeId, holder))
        }

    override suspend fun generateOffers(exchangeId: String, issuerDid: String, credentialTypes: List<String>) =
        WaltIdServices.httpWithAuth.post(
            VelocityClient.config.agentEndpoint + offersPath.format(
                VelocityClient.config.registrarApiVersion,
                issuerDid
            )
        ) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(GetOffersRequestBody(exchangeId, credentialTypes))
        }

    override suspend fun finalizeOffers(
        exchangeId: String,
        issuerDid: String,
        accepted: List<String>,
        rejected: List<String>
    ) =
        WaltIdServices.httpWithAuth.post(
            VelocityClient.config.agentEndpoint + finalizePath.format(
                VelocityClient.config.registrarApiVersion,
                issuerDid
            )
        ) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(FinalizeOfferRequestBody(exchangeId, accepted, rejected))
        }
}
