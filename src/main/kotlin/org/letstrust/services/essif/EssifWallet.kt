package org.letstrust.services.essif

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.apache.logging.log4j.Level
import org.letstrust.LetsTrustServices
import org.letstrust.common.urlEncode
import org.letstrust.crypto.encBase64
import org.letstrust.model.*
import org.letstrust.services.key.KeyService
import java.util.*

object EssifWalletClient {
    fun generateAuthenticationRequest(): String {
        return EssifWallet.generateAuthenticationRequest()
    }

    fun openSession(authResp: String): String {
        return EssifWallet.openSession(authResp)
    }
}

object EssifWallet {

    private val log = KotlinLogging.logger {}

    ///////////////////////////////////////////////////////////////////////////
    // Client
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Runs DID + VC Auth (optional)
     * Used for presenting as well as receiving VCs
     */
    fun authenticate() {

        log.debug { "CLIENT::authenticate()" }

        val authReq = EssifWalletClient.generateAuthenticationRequest()
        validateAuthenticationRequest(authReq)
        val authResp = this.generateAuthenticationResponse(authReq)
        val response = EssifWalletClient.openSession(authResp)

        log.debug { response }
        log.debug { "CLIENT::authenticate() - completed" }
    }

    private fun validateAuthenticationRequest(authReq: String): String {

        log.debug { "CLIENT::validateAuthenticationRequest()" }

        return authReq
    }

    private fun generateAuthenticationResponse(authReq: String): String {

        log.debug { "CLIENT::generateAuthenticationResponse()" }

        return authReq
    }


    ///////////////////////////////////////////////////////////////////////////
    // Server
    ///////////////////////////////////////////////////////////////////////////

    // TODO: Add configuration + keystore integration
    val kidServer = "22df3f6e54494c12bfb559e171cfe747"
    val didServer = "did:ebsi:0x416e6e6162656c2e4c65652e452d412d506f652e"
    val client_id = "http://localhost:8080/redirect" // redirect url
    val callback = client_id // same as above??

    /**
     * Generates OIDC-based authentication request
     */
    fun generateAuthenticationRequest(): String {

        log.debug { "SERVER::generateAuthenticationRequest()" }

        // TODO ingest correct parameters and claims

        val scope = "openid did_authn"
        val response_type = "id_token"
        val publicKeyJwk = Json.decodeFromString<Jwk>(KeyService.toJwk(kidServer).toPublicJWK().toString())
        val authRequestHeader = AuthenticationHeader("ES256K", "JWT", publicKeyJwk)
        val iss = didServer
        val nonce = UUID.randomUUID().toString()
        val jwks_uri = ""
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

        val didAuthOidcReq = toOidcRequest(didAuthReq)

        return Json.encodeToString(didAuthOidcReq)
    }

    /**
     * Takes the Authentication Request, verifies it and establishes a mutual-authenticated sessions.
     */
    fun openSession(authResp: String): String {

        log.debug { "SERVER::openSession()" }

        this.validateAuthenticationResponse(authResp)
        return this.generateEncryptedAccessToken(authResp)
    }

    private fun validateAuthenticationResponse(authResp: String): String {

        log.debug { "SERVER::validateAuthenticationResponse()" }

        return authResp
    }

    private fun generateEncryptedAccessToken(authResp: String): String {

        log.debug { "SERVER::generateEncryptedAccessToken()" }

        return authResp
    }

    private fun toOidcRequest(didAuthReq: DidAuthRequest): OidcRequest {
        val authRequestJwt = encBase64(Json.encodeToString(didAuthReq).toByteArray())

        val clientId = urlEncode(didAuthReq.client_id)
        val scope = urlEncode(didAuthReq.scope)

        val uri = "openid://?response_type=id_token&client_id=$clientId&scope=$scope&request=$authRequestJwt"
        return OidcRequest(uri, didAuthReq.callback)
    }
}

fun main() {
    LetsTrustServices.setLogLevel(Level.DEBUG)
    EssifWallet.authenticate()
}
