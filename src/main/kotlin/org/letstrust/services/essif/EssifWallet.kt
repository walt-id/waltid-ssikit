package org.letstrust.services.essif

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.X25519Decrypter
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator
import com.nimbusds.jose.shaded.json.JSONObject
import com.nimbusds.jwt.EncryptedJWT
import com.nimbusds.jwt.SignedJWT
import com.sksamuel.hoplite.fp.valid
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
import org.letstrust.crypto.newKeyId
import org.letstrust.model.*
import org.letstrust.services.did.DidService
import org.letstrust.services.jwt.JwtService
import org.letstrust.services.key.KeyService
import org.letstrust.services.vc.CredentialService
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
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

    val kidClient= "22df3f6e54494c12bfb559e171cfe747"
    val didClient= "did:ebsi:0x416e6e6162656c2e4c65652e452d412d506f652e"

    // SECP
    //        val kg = KeyPairGenerator.getInstance("EC", "BC")
    //        kg.initialize(ECGenParameterSpec("secp256k1"), SecureRandom())
    //        val ephPupKey = kg.generateKeyPair().let {
    //            ECKey.Builder(Curve.SECP256K1, it.public as ECPublicKey)
    //                .keyUse(KeyUse.SIGNATURE)
    //                .algorithm(JWSAlgorithm.ES256K)
    //                .keyID(newKeyId().id)
    //                .privateKey(it.private)
    //                .build()
    //        }

    // ED
    // val ephPupKey = KeyService.toJwk(KeyService.generate(KeyAlgorithm.EdDSA_Ed25519).id)

    // X25519
    // TODO move to KeyStore
    val ephPrivKey: OctetKeyPair = OctetKeyPairGenerator(Curve.X25519)
        .keyID(JwtService.keyId)
        .generate()

    /**
     * Runs DID + VC Auth (optional)
     * Used for presenting as well as receiving VCs
     */
    fun authenticate() {

        log.debug { "CLIENT::authenticate()" }

        val authReq = EssifWalletClient.generateAuthenticationRequest()
        val didAuthReq = validateAuthenticationRequest(authReq)
        val authResp = this.generateAuthenticationResponse(didAuthReq)
        val encAccessToken = EssifWalletClient.openSession(authResp)
        val accessToken = decryptAccessToken(encAccessToken)

        log.debug { "Received access token for fetching credential: $accessToken" }
        log.debug { "CLIENT::authenticate() - completed" }
    }

    private fun decryptAccessToken(encAccessToken: String): String {

        val jwt = EncryptedJWT.parse(encAccessToken)
        //TODO load enc-key based on: jwt.header.keyID
        jwt.decrypt(X25519Decrypter(ephPrivKey))
        return jwt.payload.toString()
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

        val vc = "" // TODO load VC
        val verifiedClaims = "" // TODO: build VP createVerifiedClaims(didClient, vc)

        return OidcUtil.generateOidcAuthenticationResponse(kidClient, ephPrivKey.toPublicJWK(), didClient, verifiedClaims, authReq.nonce)
    }

    private fun createVerifiedClaims(did: String, va: String): String {

        val vaWrapper = Json.decodeFromString<EbsiVAWrapper>(va)

        val vpReq = EbsiVaVp(
            listOf("https://www.w3.org/2018/credentials/v1"),
            listOf("VerifiablePresentation"),
            null,
            listOf(vaWrapper.verifiableCredential),
            did,
            null
        )

        val authKeyId = DidService.loadDidEbsi(did).authentication!![0]
        val vp = CredentialService.sign(did, vpReq.encode(), null, null, authKeyId, "assertionMethod")

        log.debug { "Verifiable Presentation generated:\n$vp" }

      //  verifiablePresentationFile.writeText(vp)

        val vpCan = canonicalize(vp)

        return encBase64Str(vpCan)
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
    EssifWallet.authenticate()
}
