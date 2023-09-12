package id.walt.services.ecosystems.essif.conformance

import com.beust.klaxon.Klaxon
import id.walt.crypto.KeyAlgorithm
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.WaltIdServices
import id.walt.services.did.DidEbsiCreateOptions
import id.walt.services.did.DidService
import id.walt.services.jwt.JwtService
import id.walt.services.key.KeyService
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.util.*

object CredentialIssuanceFlow {
    const val authorizationServer = "https://conformance-test.ebsi.eu/conformance/v3/auth-mock"
    const val authorizationEndpoint = "https://conformance-test.ebsi.eu/conformance/v3/auth-mock/authorize"
    const val credentialIssuer = "https://conformance-test.ebsi.eu/conformance/v3/issuer-mock"

    private val klaxon = Klaxon()
    private val http = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        if (WaltIdServices.httpLogging) {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }
        }
    }

    suspend fun getCredential(type: String) {
        val queryParams = authorizeRequest(type)
        val idTokenParams = directPostIdTokenRequest()
        val authToken = authTokenRequest()
        val jwtCredential = credentialRequest()
        decodeCredential(jwtCredential)
    }

    /*private */suspend fun authorizeRequest(credential: String): String {
        // create keys (ES256 & ES256k)
        val key = KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1)
        // create did
        val did = DidService.create(DidMethod.ebsi, key.id, DidEbsiCreateOptions(version = 1))
        // client-id
        val clientId = "https://conformance-test.ebsi.eu/conformance/v3/client-mock/$did"
        val scope = "openid"
        val clientMetadata = getClientMetadata(clientId)
        val authorizationDetails = listOf(getAuthorizationDetails(getCredentialRequestedTypesList(credential), credentialIssuer))
        val queryParams = mapOf(
            "scope" to scope,
            "client_id" to clientId,
            "client_metadata" to clientMetadata,
            "redirect_uri" to "$clientId/code-cb",
            "response_type" to "code",
            "state" to UUID.randomUUID().toString(),
            "authorization_details" to authorizationDetails,
            //TODO:???
//        "code_challenge" to "",
//        "code_challenge_method" to "",
//        "issuer_state" to "",
        )
        val jwtPayload = mapOf(
            "client_metadata" to clientMetadata,
            "authorization_details" to authorizationDetails
        ).plus(queryParams)
            .plus(mapOf(
                "iss" to clientId,
                "aud" to credentialIssuer
            ))
        // TODO: set issuer, set audience
        val requestParam = JwtService.getService().sign(key.id, klaxon.toJsonString(jwtPayload))
        val authResponse = http.get(authorizationEndpoint){
            url{
                queryParams.forEach{
                    parameters.append(it.key, klaxon.toJsonString(it.value))
                }
                parameters.append("request", requestParam)
            }
        }
        //TODO: parse response
        val parseResponse = authResponse.bodyAsText()
        return parseResponse
    }
    private fun directPostIdTokenRequest() {}
    private fun authTokenRequest() {}
    private fun credentialRequest(): String {
        TODO()
    }

    private fun decodeCredential(jwt: String) {}

    private fun getCredentialRequestedTypesList(type: String) = listOf(
        "VerifiableCredential", "VerifiableAttestation"
    ).apply {
        when (type) {
            "VerifiableAccreditationToAttest", "VerifiableAccreditationToAccredit" -> this.plus("VerifiableAccreditation")
            else -> {}
        }
    }.plus(type)

    private fun getClientMetadata(clientId: String) = mapOf(
        "redirect_uris" to listOf("$clientId/code-cb"),
        "jwks_uri" to "$clientId/jwks",
        "authorization_endpoint" to "$clientId/authorize"
    )

    private fun getAuthorizationDetails(credentialTypes: List<String>, credentialIssuer: String) = mapOf(
        "type" to "openid_credential",
        "format" to "jwt_vc",
        "types" to credentialTypes,
        "locations" to listOf(credentialIssuer),
    )
}
