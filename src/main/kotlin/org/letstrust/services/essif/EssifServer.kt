package org.letstrust.services.essif

import com.nimbusds.jose.crypto.X25519Decrypter
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator
import com.nimbusds.jwt.EncryptedJWT
import com.nimbusds.jwt.SignedJWT
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.apache.logging.log4j.Level
import org.letstrust.LetsTrustServices
import org.letstrust.common.OidcUtil
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.crypto.canonicalize
import org.letstrust.crypto.encBase64Str
import org.letstrust.model.*
import org.letstrust.services.did.DidService
import org.letstrust.services.jwt.JwtService
import org.letstrust.services.key.KeyService
import org.letstrust.services.vc.CredentialService
import java.util.*


object EssifServer {

    private val log = KotlinLogging.logger {}

    // TODO: Add configuration + keystore integration
    val redirectUri = "http://localhost:8080/redirect"
    val callback = "http://localhost:8080/callback"

    val nonce: String = UUID.randomUUID().toString()
    val did: String = DidService.create(DidMethod.ebsi) // Server DID

    /**
     * Generates OIDC-based authentication request
     */
    fun generateAuthenticationRequest(): String {

        log.debug { "SERVER::generateAuthenticationRequest()" }

        // TODO ingest correct parameters and claims

        val oidcRequest = OidcUtil.generateOidcAuthenticationRequest(did, redirectUri, callback, nonce)

        return Json.encodeToString(oidcRequest)
    }

    /**
     * Takes the Authentication Request, verifies it and establishes a mutual-authenticated sessions.
     */
    fun openSession(authResp: String): String {

        log.debug { "SERVER::openSession()" }

        val authRespPayload = this.validateAuthenticationResponse(authResp)
        val encryptionKeyJsonStr = Json.encodeToString(authRespPayload.claims.encryption_key)
        return this.generateEncryptedAccessToken(encryptionKeyJsonStr)
    }

    private fun validateAuthenticationResponse(authResp: String): AuthenticationResponsePayload {

        log.debug { "SERVER::validateAuthenticationResponse()" }

        // TODO validate JWT & DID & VP

        val jwt = SignedJWT.parse(authResp)

        println(jwt.payload)

        val arp = Json.decodeFromString<AuthenticationResponsePayload>(jwt.payload.toString())

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

        val privateKeyId = KeyService.generate(KeyAlgorithm.EdDSA_Ed25519).id
        println(privateKeyId)
        val encToken = JwtService.encrypt(privateKeyId, emphClientKey, accessToken)

        return encToken
    }
}

fun main() {
    LetsTrustServices.setLogLevel(Level.DEBUG)
    EssifClient.authenticate()
}
