package id.walt.common

import com.beust.klaxon.Klaxon
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import id.walt.model.*
import id.walt.services.essif.userwallet.UserWalletService
import id.walt.services.jwt.JwtService
import id.walt.services.key.KeyService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import mu.KotlinLogging
import java.time.Instant
import java.util.*

object OidcUtil {

    private val log = KotlinLogging.logger {}
    private val keyService = KeyService.getService()
    private val jwtService = JwtService.getService()

    fun generateOidcAuthenticationRequest(
        did: String,
        redirectUri: String,
        callback: String,
        nonce: String
    ): OidcRequest {

        // TODO ingest correct parameters and claims

        val scope = "openid did_authn"
        val response_type = "id_token"
        val publicKeyJwk = Klaxon().parse<Jwk>(keyService.toJwk(did).toPublicJWK().toString())!!
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
                listOf()
            )
        )
        val authRequestPayload =
            AuthenticationRequestPayload(scope, iss, response_type, client_id, nonce, registration, claims)
        val didAuthRequestJwt = AuthenticationRequestJwt(authRequestHeader, authRequestPayload)
        val didAuthReq = DidAuthRequest(response_type, client_id, scope, nonce, didAuthRequestJwt, callback)

        return toOidcRequest(didAuthReq, did)
    }

    fun validateOidcAuthenticationRequest(oidcAuthReq: OidcRequest): DidAuthRequest {

        val didAuthReq = toDidAuthRequest(oidcAuthReq)

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
        // val authRequestJwt = encBase64(Klaxon().toJsonString(didAuthReq).toByteArray())

        val payload = Klaxon().toJsonString(didAuthReq)
        val authRequestJwt = jwtService.sign(keyAlias, payload)

        val clientId = urlEncode(didAuthReq.client_id)
        val scope = urlEncode(didAuthReq.scope)

        val uri = "openid://?response_type=id_token&client_id=$clientId&scope=$scope&request=$authRequestJwt&nonce=${didAuthReq.nonce}"
        return OidcRequest(uri, didAuthReq.callback)
    }

    fun toDidAuthRequest(oidcAuthReq: OidcRequest): DidAuthRequest {

        val paramString = oidcAuthReq.uri.substringAfter("openid://?")
        val pm = toParamMap(paramString)

        val responseType = pm["response_type"]!!
        val scope = pm["scope"]!!
        val authRequestJwt = pm["request"]!!

        if (jwtService.verify(authRequestJwt)) {
            log.debug { "Successfully verified signature of JWT" }
        } else {
            throw Exception("Could not verify JWT $authRequestJwt")
        }

        //val didAuthRequestStr = String(decBase64(authRequestJwt))
        val jwt = SignedJWT.parse(authRequestJwt)

        val request = Klaxon().parse<DidAuthRequest>(jwt.payload.toString())!!

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

    fun generateOidcAuthenticationResponse(
        emphPubKey: JWK,
        did: String,
        verifiedClaims: String,
        nonce: String
    ): String {

        //val kid = DidService.loadDidEbsi(did).authentication!![0]
        //val key = emphPrivKey as ECKey
        //val key = KeyService.toJwk(did, false, kid) as ECKey

        val thumbprint = emphPubKey.computeThumbprint().toString()

        val payload = JWTClaimsSet.Builder()
            .issuer("https://self-issued.me")
            .audience("/siop-sessions")
            .subject(thumbprint)
            .issueTime(Date.from(Instant.now()))
            .expirationTime(Date.from(Instant.now().plusSeconds(300)))
            .claim("nonce", nonce)
            .claim("sub_jwk", emphPubKey.toJSONObject())
            .claim(
                "claims",
                mapOf(
                    "verified_claims" to verifiedClaims,
                    "encryption_key" to UserWalletService.embedPublicEncryptionKey(emphPubKey)
                )
            )
            .build().toString()

        val jwt = jwtService.sign(did, payload)

        jwtService.verify(jwt).let { if (!it) throw IllegalStateException("Generated JWK not valid") }

        return jwt
    }

}
