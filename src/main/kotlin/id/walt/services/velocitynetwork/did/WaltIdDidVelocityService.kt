package id.walt.services.velocitynetwork.did

import id.walt.model.DidUrl
import id.walt.services.WaltIdServices
import id.walt.services.velocitynetwork.VelocityNetwork
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
        WaltIdServices.httpWithAuth.get(VelocityNetwork.VELOCITY_NETWORK_REGISTRAR_ENDPOINT + didResolvePath.format(VelocityNetwork.AGENT_API_VERSION, didUrl.did)) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }

    override suspend fun onboard(orgData: String) =
        WaltIdServices.httpWithAuth.post(VelocityNetwork.VELOCITY_NETWORK_REGISTRAR_API + registerOrganizationPath.format(VelocityNetwork.REGISTRAR_API_VERSION)) {
            setBody(orgData)
        }
}