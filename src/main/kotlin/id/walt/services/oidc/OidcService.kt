package id.walt.services.oidc

import com.nimbusds.oauth2.sdk.ResponseMode
import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import com.nimbusds.oauth2.sdk.util.URLUtils
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse
import id.walt.common.prettyPrint
import id.walt.custodian.Custodian
import id.walt.model.DidMethod
import id.walt.model.oidc.CredentialAuthorizationDetails
import id.walt.model.oidc.IssuanceInitiationRequest
import id.walt.model.oidc.OIDCProvider
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.vclib.credentials.VerifiablePresentation
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.model.toCredential
import java.net.URI
import java.util.*

object OidcService {

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
                        "${client_id?.let { " --client-id $client_id" } ?: ""}" +
                        "${client_secret?.let { " --client-secret $client_secret" } ?: ""}" +
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
                        "${client_id?.let { " --client-id $client_id" } ?: ""}" +
                        "${client_secret?.let { " --client-secret $client_secret" } ?: ""}" +
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
                        "${client_id?.let { " --client-id $client_id" } ?: ""}" +
                        "${client_secret?.let { " --client-secret $client_secret" } ?: ""}" +
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

        println("Access token response:")
        val jsonObj =
            OIDC4CIService.getAccessToken(issuer, authCode, redirect_uri.substringBeforeLast("?"), isPreAuthorized, userPin)
                .toJSONObject()
        println(jsonObj.prettyPrint())
        println()
        println("Now get the credential using:")
        println(
            "ssikit oidc ci credential -i $issuer_url -t ${jsonObj.get("access_token") ?: "<token>"} ${
                jsonObj.get("c_nonce")?.let { "-n $it" } ?: ""
            } -d <subject did> -s <credential schema id>")
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

        val issuanceInitiationRequest =
            IssuanceInitiationRequest.fromQueryParams(URLUtils.parseParameters(URI.create(uri).query))
        val issuer_url = issuanceInitiationRequest.issuer_url
        val isPreAuthorized = issuanceInitiationRequest.isPreAuthorized

        val tokenResponse = if (!isPreAuthorized) {
            authorization(issuanceInitiationRequest, client_id, client_secret, nonce, format)
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

        val tokenJson = tokenResponse.toJSONObject()
        val token = tokenJson["access_token"] as? String ?: "<token>"
        nonce = tokenJson["c_nonce"]?.let { "-n $it" } ?: ""


        val vc = credential(
            issuer_url,
            client_id,
            client_secret,
            did,
            token,
            nonce,
            issuanceInitiationRequest.credential_types.joinToString(", "),
            format
        )

        vc.id = vc.id ?: UUID.randomUUID().toString()
        Custodian.getService().storeCredential(vc.id!!, vc)

        return vc.id!!

    }

    /*fun vpParse() {

    }*/

    fun present(authUrl: String, did: String, credentialIds: List<String>): String {
        val mode: CompatibilityMode = CompatibilityMode.OIDC

        val req = OIDC4VPService.parseOIDC4VPRequestUri(URI.create(authUrl))
        val nonce = req.getCustomParameter("nonce")?.firstOrNull()
        val vp = Custodian.getService().createPresentation(
            credentialIds.map { Custodian.getService().getCredential(it)!!.encode() },
            did,
            challenge = nonce,
            expirationDate = null
        ).toCredential() as VerifiablePresentation
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

    val vcId = OidcService.issuance(
        "openid-initiate-issuance://?issuer=https%3A%2F%2Fissuer.walt-test.cloud%2Fissuer-api%2Foidc%2F&credential_type=VerifiableId&pre-authorized_code=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIwNjRjOGE5Yi0xMjEzLTQ3OTItYWU0Yy0xYzMyYzJlOGY2N2IiLCJwcmUtYXV0aG9yaXplZCI6dHJ1ZX0.KavupJvOPkBpA7N7AO7BbmwY_yf-QMeKtcSuKuiUze0&user_pin_required=false",
        did
    )
    OidcService.present(
        "openid:/?scope=openid&presentation_definition=%7B%22format%22+%3A+null%2C+%22id%22+%3A+%221%22%2C+%22input_descriptors%22+%3A+%5B%7B%22constraints%22+%3A+null%2C+%22format%22+%3A+null%2C+%22group%22+%3A+null%2C+%22id%22+%3A+%221%22%2C+%22name%22+%3A+null%2C+%22purpose%22+%3A+null%2C+%22schema%22+%3A+%7B%22uri%22+%3A+%22https%3A%2F%2Fapi.preprod.ebsi.eu%2Ftrusted-schemas-registry%2Fv1%2Fschemas%2F0xb77f8516a965631b4f197ad54c65a9e2f9936ebfb76bae4906d33744dbcc60ba%22%7D%7D%5D%2C+%22name%22+%3A+null%2C+%22purpose%22+%3A+null%2C+%22submission_requirements%22+%3A+null%7D&response_type=vp_token&redirect_uri=https%3A%2F%2Fverifier.walt-test.cloud%2Fverifier-api%2Fverify&state=c46e2e1b-b8e6-4f9a-80a1-1b093a6919f9&nonce=c46e2e1b-b8e6-4f9a-80a1-1b093a6919f9&client_id=https%3A%2F%2Fverifier.walt-test.cloud%2Fverifier-api%2Fverify&response_mode=post",
        did, listOf(vcId)
    )
}
