package id.walt.services.ecosystems.essif

import com.beust.klaxon.Klaxon
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jwt.SignedJWT
import id.walt.common.OidcUtil
import id.walt.crypto.KeyAlgorithm
import id.walt.model.AuthenticationResponsePayload
import id.walt.model.DidMethod
import id.walt.services.did.DidService
import id.walt.services.jwt.JwtService
import id.walt.services.key.KeyService
import mu.KotlinLogging
import java.util.*

object EssifServer {

    private val log = KotlinLogging.logger {}
    private val keyService = KeyService.getService()
    private val jwtService = JwtService.getService()

    // TODO: Add configuration + keystore integration
    val redirectUri = "http://localhost:8080/redirect"
    val callback = "http://localhost:8080/callback"

    val nonce: String = UUID.randomUUID().toString()

    /**
     * Generates OIDC-based authentication request
     */
    fun generateAuthenticationRequest(): String {

        log.debug { "SERVER::generateAuthenticationRequest()" }

        // TODO ingest correct parameters and claims

        val did: String = DidService.create(DidMethod.ebsi) // Server DID

        val oidcRequest = OidcUtil.generateOidcAuthenticationRequest(did, redirectUri, callback, nonce)

        return Klaxon().toJsonString(oidcRequest)
    }

    /**
     * Takes the Authentication Request, verifies it and establishes a mutual-authenticated sessions.
     */
    fun openSession(authRespJwt: String): String {

        log.debug { "SERVER::openSession()" }

        val authRespPayload = this.validateAuthenticationResponse(authRespJwt)
        val encryptionKeyJsonStr = Klaxon().toJsonString(authRespPayload.claims.encryption_key)
        return this.generateEncryptedAccessToken(encryptionKeyJsonStr)
    }

    private fun validateAuthenticationResponse(authResp: String): AuthenticationResponsePayload {

        log.debug { "SERVER::validateAuthenticationResponse()" }

        // TODO validate JWT & DID & VP

        val jwt = SignedJWT.parse(authResp)

        println(jwt.payload)

        val arp = Klaxon().parse<AuthenticationResponsePayload>(jwt.payload.toString())!!

        return arp

//        val claims = jwt.jwtClaimsSet.getClaim("claims") as JSONObject
//
//        val vp = claims.get("verified_claims")
//        val encryptionKey = claims.get("encryption_key") as JSONObject
//
//        println(vp)
//
//        return encryptionKey.toString()
    }

    private fun generateEncryptedAccessToken(encryptionKey: String): String {

        log.debug { "SERVER::generateEncryptedAccessToken()" }

        // Generate Access token
        val accessToken = UUID.randomUUID().toString()

        // Encrypt JWE
        println(encryptionKey)
        val emphClientKey = OctetKeyPair.parse(encryptionKey) // ECKey.parse(encryption_key)

        val privateKeyId = keyService.generate(KeyAlgorithm.EdDSA_Ed25519).id
        println(privateKeyId)
        val encToken = jwtService.encrypt(privateKeyId, emphClientKey, accessToken)

        return encToken
    }
}
