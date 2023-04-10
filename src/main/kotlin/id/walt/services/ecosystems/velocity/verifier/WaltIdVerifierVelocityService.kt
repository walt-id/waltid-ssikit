package id.walt.services.ecosystems.velocity.verifier

import id.walt.services.WaltIdServices
import id.walt.services.ecosystems.velocity.VelocityClient
import id.walt.services.ecosystems.velocity.models.CheckCredentialRequest
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
            VelocityClient.config.agentEndpoint + inspectionPath.format(VelocityClient.config.agentApiVersion, issuerDid)) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(CheckCredentialRequest(listOf(CheckCredentialRequest.RawCredential(credential))))
        }
}
