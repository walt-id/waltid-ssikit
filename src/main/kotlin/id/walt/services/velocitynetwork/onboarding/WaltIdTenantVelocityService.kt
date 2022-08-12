package id.walt.services.velocitynetwork.onboarding

import com.beust.klaxon.Klaxon
import id.walt.services.WaltIdServices
import id.walt.services.velocitynetwork.VelocityClient
import id.walt.services.velocitynetwork.models.requests.CreateDisclosureRequest
import id.walt.services.velocitynetwork.models.requests.CreateTenantRequest
import io.ktor.client.request.*
import io.ktor.http.*

class WaltIdTenantVelocityService: TenantVelocityService() {
    companion object{
        const val createTenantPath = "/operator-api/%s/tenants"
        const val addDisclosurePath = "/operator-api/%s/tenants/%s/disclosures"
    }

    override suspend fun create(tenantData: String) =
        WaltIdServices.httpWithAuth.post(VelocityClient.config.agentEndpoint + createTenantPath.format(VelocityClient.config.agentApiVersion)) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(Klaxon().parse<CreateTenantRequest>(tenantData))
        }

    override suspend fun addDisclosure(did: String, disclosureData: String) =
        WaltIdServices.httpWithAuth.post(
            VelocityClient.config.agentEndpoint + addDisclosurePath.format(VelocityClient.config.agentApiVersion, did)) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(Klaxon().parse<CreateDisclosureRequest>(disclosureData))
        }
}