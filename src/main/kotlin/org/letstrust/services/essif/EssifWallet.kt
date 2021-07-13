package org.letstrust.services.essif

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.apache.logging.log4j.Level
import org.letstrust.LetsTrustServices
import org.letstrust.common.OidcUtil
import org.letstrust.model.*
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
        val didAuthReq = validateAuthenticationRequest(authReq)
        val authResp = this.generateAuthenticationResponse(didAuthReq)
        val response = EssifWalletClient.openSession(authResp)

        log.debug { response }
        log.debug { "CLIENT::authenticate() - completed" }
    }

    private fun validateAuthenticationRequest(authReq: String): DidAuthRequest {

        log.debug { "CLIENT::validateAuthenticationRequest()" }

        val oidcReq = Json.decodeFromString<OidcRequest>(authReq)

        val didAuthReq = OidcUtil.validateOidcAuthenticationRequest(oidcReq)

        // TODO: consider further validation steps

        return didAuthReq
    }

    private fun generateAuthenticationResponse(authReq: DidAuthRequest): String {

        log.debug { "CLIENT::generateAuthenticationResponse()" }

        return "TODO"
    }


    ///////////////////////////////////////////////////////////////////////////
    // Server
    ///////////////////////////////////////////////////////////////////////////

    // TODO: Add configuration + keystore integration
    val kidServer = "22df3f6e54494c12bfb559e171cfe747"
    val didServer = "did:ebsi:0x416e6e6162656c2e4c65652e452d412d506f652e"
    val redirectUri = "http://localhost:8080/redirect"
    val callback = "http://localhost:8080/callback"
    val nonce = UUID.randomUUID().toString()

    /**
     * Generates OIDC-based authentication request
     */
    fun generateAuthenticationRequest(): String {

        log.debug { "SERVER::generateAuthenticationRequest()" }

        // TODO ingest correct parameters and claims

        val oidcRequest = OidcUtil.generateOidcAuthenticationRequest(kidServer, didServer, redirectUri, callback, nonce)

        return Json.encodeToString(oidcRequest)
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
}

fun main() {
    LetsTrustServices.setLogLevel(Level.DEBUG)
    EssifWallet.authenticate()
}
