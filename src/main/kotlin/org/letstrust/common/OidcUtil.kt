package org.letstrust.common

import com.nimbusds.jwt.SignedJWT
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import mu.KotlinLogging
import org.letstrust.model.*
import org.letstrust.services.jwt.JwtService
import org.letstrust.services.key.KeyService

object OidcUtil {

    private val log = KotlinLogging.logger {}

    fun generateOidcAuthenticationRequest(kid: String, did: String, redirectUri: String, callback: String, nonce: String): OidcRequest {

        // TODO ingest correct parameters and claims

        val scope = "openid did_authn"
        val response_type = "id_token"
        val publicKeyJwk = Json.decodeFromString<Jwk>(KeyService.toJwk(kid).toPublicJWK().toString())
        val authRequestHeader = AuthenticationHeader("ES256K", "JWT", publicKeyJwk)
        val iss = did
        val jwks_uri = ""
        val client_id = redirectUri
        val registration = AuthenticationRequestRegistration(
            listOf("https://app.ebsi.xyz"),
            response_type,
            listOf("ES25K", "EdDSA"),
            listOf("ES25K", "EdDSA"),
            listOf("ES25K", "EdDSA"),
            listOf("ECDH-ES"),
            listOf("A128GCM", "A256GCM"),
            jwks_uri
        )
        val claims = Claim(
            IdToken(
                listOf<String>()
            )
        )
        val authRequestPayload = AuthenticationRequestPayload(scope, iss, response_type, client_id, nonce, registration, claims)
        val didAuthRequestJwt = AuthenticationRequestJwt(authRequestHeader, authRequestPayload)
        val didAuthReq = DidAuthRequest(response_type, client_id, scope, nonce, didAuthRequestJwt, callback)

        return toOidcRequest(didAuthReq, kid)
    }

    fun validateOidcAuthenticationRequest(oidcAuthReq: OidcRequest): DidAuthRequest {

        val didAuthReq = toDidAuthRequest(oidcAuthReq)

        println(didAuthReq)

        // TODO: Validate DID

        return didAuthReq
    }

    fun toOidcAuthenticationRequestUri(authenticationRequestResponseJson: String): OidcAuthenticationRequestUri {
        try {
            val uri = Json.parseToJsonElement(authenticationRequestResponseJson).jsonObject["uri"].toString()
            val paramString = uri.substringAfter("openid://?")
            val pm = toParamMap(paramString)
            return OidcAuthenticationRequestUri(pm["response_type"]!!, pm["scope"]!!, pm["request"]!!)
        } catch (e: Exception) {
            log.error { "Could not parse AuthenticationRequestResponse: $authenticationRequestResponseJson" }
            throw e
        }
    }

    fun toOidcRequest(didAuthReq: DidAuthRequest, keyAlias: String): OidcRequest {
        // val authRequestJwt = encBase64(Json.encodeToString(didAuthReq).toByteArray())

        val payload = Json.encodeToString(didAuthReq)
        val authRequestJwt = JwtService.sign(keyAlias, payload)

        val clientId = urlEncode(didAuthReq.client_id)
        val scope = urlEncode(didAuthReq.scope)

        val uri = "openid://?response_type=id_token&client_id=$clientId&scope=$scope&request=$authRequestJwt"
        return OidcRequest(uri, didAuthReq.callback)
    }

    fun toDidAuthRequest(oidcAuthReq: OidcRequest): DidAuthRequest {

        val paramString = oidcAuthReq.uri.substringAfter("openid://?")
        val pm = toParamMap(paramString)

        val responseType = pm["response_type"]!!
        val scope = pm["scope"]!!
        val authRequestJwt = pm["request"]!!

        if (JwtService.verify(authRequestJwt)) {
            log.debug { "Successfully verified signature of JWT" }
        } else {
            throw Exception("Could not verify JWT $authRequestJwt")
        }

        //val didAuthRequestStr = String(decBase64(authRequestJwt))
        val jwt = SignedJWT.parse(authRequestJwt)

        val request = Json.decodeFromString<DidAuthRequest>(jwt.payload.toString())

        if (oidcAuthReq.callback != request.callback) {
            throw Exception("Callbacks in OidcRequest data structure are not matching we got: ${oidcAuthReq.callback} & ${request.callback}")
        }

        if (scope != request.scope) {
            throw Exception("Scopes in OidcRequest data structure are not matching we got: ${scope} & ${request.scope}")
        }

        if (responseType != request.reponse_type) {
            throw Exception("Scopes in OidcRequest data structure are not matching we got: ${responseType} & ${request.reponse_type}")
        }

        return request

        //  return DidAuthRequest(responseType, clientId, scope, nonce, request, oidcAuthReq.callback)
    }

}
