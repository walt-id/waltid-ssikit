package id.walt.services.velocitynetwork.verifier

import id.walt.services.WaltIdServices
import id.walt.services.velocitynetwork.VelocityNetwork
import id.walt.services.velocitynetwork.models.requests.CheckCredentialRequest
import io.ktor.client.request.*
import io.ktor.http.*
import mu.KotlinLogging

class WaltIdVerifierVelocityService: VerifierVelocityService() {
    companion object{
        const val inspectionPath = "/operator-api/%s/tenants/%s/check-credentials"
    }
    private val log = KotlinLogging.logger {}

    override suspend fun check(issuerDid: String, credential: String) =
        WaltIdServices.httpWithAuth.post(
            VelocityNetwork.agentUrl + inspectionPath.format(VelocityNetwork.AGENT_API_VERSION, issuerDid)) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(CheckCredentialRequest(listOf(CheckCredentialRequest.RawCredential(credential))))
        }
}