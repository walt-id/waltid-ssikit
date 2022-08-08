package id.walt.services.velocitynetwork.did

import id.walt.model.Did
import id.walt.model.DidUrl
import id.walt.model.DidVelocity
import id.walt.services.WaltIdServices
import id.walt.services.velocitynetwork.VelocityNetwork
import id.walt.services.velocitynetwork.models.responses.CreateOrganizationResponse
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import mu.KotlinLogging

class WaltIdDidVelocityService : DidVelocityService() {

    companion object {
        const val didResolvePath = "api/v0.6/resolve-did/%s"
        const val registerOrganizationPath = "/api/v0.6/organizations/full"
    }

    private val log = KotlinLogging.logger {}

    override suspend fun resolveDid(didUrl: DidUrl) =
        WaltIdServices.httpWithAuth.get(VelocityNetwork.VELOCITY_NETWORK_REGISTRAR_ENDPOINT + didResolvePath.format(didUrl.did)) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }.body<Did>() as DidVelocity

    override suspend fun onboard(orgData: String) =
        WaltIdServices.httpWithAuth.post(VelocityNetwork.VELOCITY_NETWORK_REGISTRAR_API + registerOrganizationPath) {
            setBody(orgData)
        }.bodyAsText()
}