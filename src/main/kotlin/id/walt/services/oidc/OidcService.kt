package id.walt.services.oidc

import com.nimbusds.oauth2.sdk.ResponseMode
import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import com.nimbusds.oauth2.sdk.util.URLUtils
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse
import id.walt.common.prettyPrint
import id.walt.credentials.w3c.PresentableCredential
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.toVerifiablePresentation
import id.walt.custodian.Custodian
import id.walt.model.DidMethod
import id.walt.model.oidc.CredentialAuthorizationDetails
import id.walt.model.oidc.IssuanceInitiationRequest
import id.walt.model.oidc.OIDCProvider
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import mu.KotlinLogging
import java.net.URI
import java.util.*

object OidcService {

    private val log = KotlinLogging.logger { }

    fun authorization(
        issuanceInitiationRequest: IssuanceInitiationRequest,
        client_id: String?,
        client_secret: String?,
        nonce: String?,
        format: String
    ) {
        val issuer_url = issuanceInitiationRequest.issuer_url
        val credential_types = issuanceInitiationRequest.credential_types
        val mode = "push" // push|get|redirect
        val redirect_uri = "http://blank"

        val issuer = OIDC4CIService.getWithProviderMetadata(
            OIDCProvider(
                issuer_url,
                issuer_url,
                client_id = client_id,
                client_secret = client_secret
            )
        )
        val credentialDetails = credential_types.map { CredentialAuthorizationDetails(credential_type = it, format = format) }

        when (mode) {
            "get" -> {
                val redirectUri = OIDC4CIService.executeGetAuthorizationRequest(
                    issuer,
                    URI.create(redirect_uri),
                    credentialDetails,
                    nonce = nonce
                )
                println()
                println("Client redirect URI:")
                println(redirectUri)
                println()
                println("Now get the token using:")
                println("ssikit oidc ci token -i $issuer_url" +
                        (client_id?.let { " --client-id $client_id" } ?: "") +
                        (client_secret?.let { " --client-secret $client_secret" } ?: "") +
                        " -m ebsi_wct -r \"$redirectUri\"")
            }

            "redirect" -> {
                val userAgentUri = OIDC4CIService.getUserAgentAuthorizationURL(
                    issuer,
                    URI.create(redirect_uri),
                    credentialDetails,
                    nonce = nonce
                )
                println()
                println("Point your browser to this address and authorize with the issuer:")
                println(userAgentUri)
                println()
                println("Then paste redirection url from browser to this command to retrieve the access token:")
                println("ssikit oidc ci token -i $issuer_url" +
                        (client_id?.let { " --client-id $client_id" } ?: "") +
                        (client_secret?.let { " --client-secret $client_secret" } ?: "") +
                        " -r <url from browser>")

            }

            else -> {
                val userAgentUri = OIDC4CIService.executePushedAuthorizationRequest(
                    issuer,
                    URI.create(redirect_uri),
                    credentialDetails,
                    nonce = nonce
                )
                println()
                println("Point your browser to this address and authorize with the issuer:")
                println(userAgentUri)
                println()
                println("Then paste redirection url from browser to this command to retrieve the access token:")
                println("ssikit oidc ci token -i $issuer_url" +
                        (client_id?.let { " --client-id $client_id" } ?: "") +
                        (client_secret?.let { " --client-secret $client_secret" } ?: "") +
                        " -r <url from browser>")
            }
        }
    }

    fun token(
        issuer_url: String,
        client_id: String?,
        client_secret: String?,
        code: String?,
        redirect_uri: String,
        isPreAuthorized: Boolean,
        userPin: String?
    ): OIDCTokenResponse {
        val issuer = OIDC4CIService.getWithProviderMetadata(
            OIDCProvider(
                issuer_url,
                issuer_url,
                client_id = client_id,
                client_secret = client_secret
            )
        )
        val authCode = code ?: OIDCUtils.getCodeFromRedirectUri(URI.create(redirect_uri))
        ?: throw IllegalArgumentException("Error: Auth code not specified!")

        return OIDC4CIService.getAccessToken(issuer, authCode, redirect_uri.substringBeforeLast("?"), isPreAuthorized, userPin)
    }

    fun credential(
        issuer_url: String,
        client_id: String?,
        client_secret: String?,
        did: String,
        token: String,
        nonce: String,
        schemaId: String,
        format: String
    ): VerifiableCredential {
        val issuer = OIDC4CIService.getWithProviderMetadata(
            OIDCProvider(
                issuer_url,
                issuer_url,
                client_id = client_id,
                client_secret = client_secret
            )
        )

        val proof = OIDC4CIService.generateDidProof(issuer, did, nonce)
        val vc = OIDC4CIService.getCredential(issuer, BearerAccessToken(token), schemaId, proof, format)
            ?: throw IllegalStateException("Error: no credential received")

        return vc
    }

    fun issuance(uri: String, did: String): String {
        val client_id = null
        val client_secret = null
        var nonce: String? = null
        val format = "ldp_vc" // or jwt_vc

        log.debug { "Parsing issuance initiation request..." }
        val issuanceInitiationRequest =
            IssuanceInitiationRequest.fromQueryParams(URLUtils.parseParameters(URI.create(uri).query))
        val issuer_url = issuanceInitiationRequest.issuer_url

        log.debug { "Issuer url: $issuer_url" }
        val isPreAuthorized = issuanceInitiationRequest.isPreAuthorized
        log.debug { "Preauthroized: $isPreAuthorized" }

        val tokenResponse = if (!isPreAuthorized) {
            log.debug { "Not pre-authorized, authorizing..." }
            authorization(issuanceInitiationRequest, client_id, client_secret, nonce, format)
            log.debug { "Getting token..." }
            token(
                issuer_url = issuer_url,
                client_id = client_id,
                client_secret = client_secret,
                code = null,
                redirect_uri = "http://blank",
                isPreAuthorized = false,
                userPin = null
            )
        } else {
            val userPin = null
            log.debug { "Pre-authorized, can continue fetching token..." }
            token(
                issuer_url = issuer_url,
                client_id = client_id,
                client_secret = client_secret,
                code = issuanceInitiationRequest.pre_authorized_code,
                redirect_uri = "http://blank",
                isPreAuthorized = true,
                userPin = userPin
            )
        }
        log.debug { "Received token!" }

        log.debug { "Parsing token..." }
        val tokenJson = tokenResponse.toJSONObject()
        val token = tokenJson["access_token"] as? String ?: "<token>"
        nonce = tokenJson["c_nonce"]?.let { "-n $it" } ?: ""
        val schemaId = issuanceInitiationRequest.credential_types.joinToString(", ")

        log.debug { "Credential request = issuer: $issuer_url, clientId: $client_id, clientSecret: $client_secret, nonce: $nonce" }
        log.debug { "Credential request DID: $did" }
        log.debug { "Credential request token: $token" }
        log.debug { "Credential request schema: $schemaId, format: $format" }

        log.debug { "Requesting credential with token..." }
        val vc = credential(
            issuer_url = issuer_url,
            client_id = client_id,
            client_secret = client_secret,
            did = did,
            token = token,
            nonce = nonce,
            schemaId = schemaId,
            format = format
        )
        log.debug { "Received credential!" }

        vc.id = vc.id ?: UUID.randomUUID().toString()

        log.debug { "Storing credential \"${vc.id}\" in Custodian..." }
        Custodian.getService().storeCredential(vc.id!!, vc)

        log.debug { "Credential received." }

        return vc.id!!
    }

    /*fun vpParse() {

    }*/

    fun present(authUrl: String, did: String, credentialIds: List<String>): String {
        val mode: CompatibilityMode = CompatibilityMode.OIDC

        val req = OIDC4VPService.parseOIDC4VPRequestUri(URI.create(authUrl))
        val nonce = req.getCustomParameter("nonce")?.firstOrNull()
        val vp = Custodian.getService().createPresentation(
            credentialIds
                .map { Custodian.getService().getCredential(it) ?: throw Exception("Credential with ID $it not found") }
                .map { PresentableCredential(it) },
            did,
            challenge = nonce,
        ).toVerifiablePresentation()
        val resp = OIDC4VPService.getSIOPResponseFor(req, did, listOf(vp))
        println("Presentation response:")
        println(resp.toFormParams().prettyPrint())

        println()
        if (setOf(ResponseMode.FORM_POST, ResponseMode("post")).contains(req.responseMode)) { // "post" or "form_post"
            val result = OIDC4VPService.postSIOPResponse(req, resp, mode)
            println()
            println("Response:")
            println(result)
            return result
        } else {
            println("Redirect to:")
            println(
                "${req.redirectionURI}${
                    when (req.responseMode) {
                        ResponseMode.FRAGMENT -> "#"; else -> "?"
                    }
                }${resp.toFormBody()}"
            )
            return "${req.redirectionURI}${
                when (req.responseMode) {
                    ResponseMode.FRAGMENT -> "#"; else -> "?"
                }
            }${resp.toFormBody()}"
        }
    }
}

fun main() {
    ServiceMatrix("service-matrix.properties")

    val did = DidService.create(DidMethod.key)
    //val did = "did:iota:DVF9yjZBtSAPGzYj8x5rfHPS5XCNJ5dvvpKVmKRNYsd9#ca7d7e0205a14763a4f063a9acffb5d1"

    println("Issuance:")
    val vcId = OidcService.issuance(
        "openid-initiate-issuance://?issuer=https%3A%2F%2Fissuer.walt-test.cloud%2Fissuer-api%2Foidc%2F&credential_type=VerifiableId&pre-authorized_code=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhOGU1MzQ0YS0yYTAzLTRkNWEtYjY5Mi01ZWNkNTBhMzJjNjMiLCJwcmUtYXV0aG9yaXplZCI6dHJ1ZX0.8f6-AgiKMciZ0KEo-xLobAxAma1iZvAcRhWq3iwfUb4&user_pin_required=false",
        did
    )

    //val vcId = Custodian.getService().listCredentialIds().first()
    /*OidcService.present(
        "http://localhost:8080/sharecredential?scope=openid&presentation_definition=%7B%22format%22+%3A+null%2C+%22id%22+%3A+%221%22%2C+%22input_descriptors%22+%3A+%5B%7B%22constraints%22+%3A+%7B%22fields%22+%3A+%5B%7B%22filter%22+%3A+%7B%22const%22%3A+%22VerifiableId%22%7D%2C+%22id%22+%3A+%221%22%2C+%22path%22+%3A+%5B%22%24.type%22%5D%2C+%22purpose%22+%3A+null%7D%5D%7D%2C+%22format%22+%3A+null%2C+%22group%22+%3A+%5B%22A%22%5D%2C+%22id%22+%3A+%220%22%2C+%22name%22+%3A+null%2C+%22purpose%22+%3A+null%2C+%22schema%22+%3A+null%7D%5D%2C+%22name%22+%3A+null%2C+%22purpose%22+%3A+null%2C+%22submission_requirements%22+%3A+%5B%7B%22count%22+%3A+null%2C+%22from%22+%3A+%22A%22%2C+%22from_nested%22+%3A+null%2C+%22max%22+%3A+null%2C+%22min%22+%3A+null%2C+%22name%22+%3A+null%2C+%22purpose%22+%3A+null%2C+%22rule%22+%3A+%22all%22%7D%5D%7D&response_type=vp_token&redirect_uri=http%3A%2F%2Flocalhost%3A4000%2Fapi%2Fsiop%2Fverify&state=eyJpZHBTZXNzaW9uSWQiIDogIjI0ZjFmYWJiLWEzNGMtNDUwOS05ZDk2LTc1YTc3ZThkN2UxYyIsICJpZHBUeXBlIiA6ICJPSURDIn0%3D&nonce=3db82a1e-5aed-4bf5-87bb-95cff04c19f3&client_id=http%3A%2F%2Flocalhost%3A3000%2Fapi%2Fsiop%2Fverify&response_mode=form_post",
        did, listOf(vcId)
    )*/
    println("VC ID: $vcId")
}
