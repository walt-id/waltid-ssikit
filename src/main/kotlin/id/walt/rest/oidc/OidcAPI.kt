package id.walt.rest.oidc

import id.walt.services.oidc.OidcService
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder
import kotlinx.serialization.Serializable

object OidcAPI {

    @Serializable
    data class CiRequest(val uri: String, val did: String)

    @Serializable
    data class VpRequest(val uri: String, val did: String, val credentialIds: List<String>)

    internal const val DEFAULT_OIDC_API_PORT = 7010
    internal const val DEFAULT_BIND_ADDRESS = "127.0.0.1"


    private data class VerifiableCredentialItem(
        val type: String,
        val id: String,
        val issuer: String,
        val issuedOn: String
    )

    /**
     * Start OIDC REST API
     * @param apiTargetUrls (optional): add URLs to Swagger documentation for easy testing
     * @param bindAddress (default: 127.0.0.1): select address to bind on to, e.g. 0.0.0.0 for all interfaces
     * @param port (default: 7001): select port to listen on
     */
    fun start(
        port: Int = DEFAULT_OIDC_API_PORT,
        bindAddress: String = DEFAULT_BIND_ADDRESS,
        apiTargetUrls: List<String> = listOf()
    ) {

        Javalin.create {
            it.enableDevLogging()
        }.routes {
            ApiBuilder.post("vp") {
                val req = it.bodyAsClass<VpRequest>()
                val res = OidcService.present(req.uri, req.did, req.credentialIds)

                it.result(res)
            }
            ApiBuilder.post("ci") {
                val req = it.bodyAsClass<CiRequest>()
                val res = OidcService.issuance(req.uri, req.did)

                it.result(res)
            }
        }.start(bindAddress, port)
    }
}
