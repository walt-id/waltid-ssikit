package id.walt.services.ecosystems.velocity.did

import id.walt.model.DidUrl
import id.walt.services.WaltIdServices
import id.walt.services.ecosystems.velocity.VelocityClient
import io.ktor.client.request.*
import io.ktor.http.*
import mu.KotlinLogging

class WaltIdDidVelocityService : DidVelocityService() {

    companion object {
        const val didResolvePath = "api/%s/resolve-did/%s"
        const val registerOrganizationPath = "api/%s/organizations/full"
    }

    private val log = KotlinLogging.logger {}

    override suspend fun resolveDid(didUrl: DidUrl) =
        WaltIdServices.httpWithAuth.get(VelocityClient.config.registrarEndpoint + didResolvePath.format(VelocityClient.config.registrarApiVersion, didUrl.did)) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }

    override suspend fun onboard(orgData: String) =
        WaltIdServices.httpWithAuth.post(VelocityClient.config.registrarEndpoint + registerOrganizationPath.format(VelocityClient.config.registrarApiVersion)) {
            setBody(orgData)
        }
}
