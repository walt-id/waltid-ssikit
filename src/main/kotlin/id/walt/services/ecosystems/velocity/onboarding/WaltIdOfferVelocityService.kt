package id.walt.services.ecosystems.velocity.onboarding

import com.beust.klaxon.Klaxon
import id.walt.services.WaltIdServices
import id.walt.services.ecosystems.velocity.VelocityClient
import id.walt.services.ecosystems.velocity.models.CredentialOffer
import id.walt.services.ecosystems.velocity.models.ExchangeType
import id.walt.services.ecosystems.velocity.models.IssuerExchangeRequestBody
import io.ktor.client.request.*
import io.ktor.http.*
import mu.KotlinLogging

class WaltIdOfferVelocityService: OfferVelocityService() {
    companion object {
        const val exchangePath = "/operator-api/%s/tenants/%s/exchanges"
        const val offerPath = "/operator-api/%s/tenants/%s/exchanges/%s/offers"
        const val completePath = "/operator-api/%s/tenants/%s/exchanges/%s/offers/complete"
        const val claimPath = "/operator-api/%s/tenants/%s/exchanges/%s/qrcode.uri"
    }

    private val log = KotlinLogging.logger {}

    override suspend fun initExchange(issuerDid: String, disclosureId: String, idMatcher: List<String>) =
        WaltIdServices.httpWithAuth.post(
            VelocityClient.config.agentEndpoint + exchangePath.format(
                VelocityClient.config.agentApiVersion,
                issuerDid
            )
        ) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(IssuerExchangeRequestBody(ExchangeType.ISSUING.name, disclosureId, idMatcher))
        }

    override suspend fun addOffer(issuerDid: String, exchangeId: String, offer: String) =
        WaltIdServices.httpWithAuth.post(
            VelocityClient.config.agentEndpoint + offerPath.format(
                VelocityClient.config.agentApiVersion,
                issuerDid,
                exchangeId
            )
        ) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(Klaxon().parse<CredentialOffer>(offer))
        }

    override suspend fun completeOffer(issuerDid: String, exchangeId: String) =
        WaltIdServices.httpWithAuth.post(
            VelocityClient.config.agentEndpoint + completePath.format(
                VelocityClient.config.agentApiVersion,
                issuerDid,
                exchangeId
            )
        )

    override suspend fun claimOffer(issuerDid: String, exchangeId: String) =
        WaltIdServices.httpWithAuth.get(
            VelocityClient.config.agentEndpoint + claimPath.format(
                VelocityClient.config.agentApiVersion,
                issuerDid,
                exchangeId
            )
        )
}
