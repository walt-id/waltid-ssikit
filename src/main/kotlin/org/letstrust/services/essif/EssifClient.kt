package org.letstrust.services.essif

import com.nimbusds.jose.crypto.X25519Decrypter
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator
import com.nimbusds.jwt.EncryptedJWT
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.letstrust.common.OidcUtil
import org.letstrust.crypto.canonicalize
import org.letstrust.crypto.encBase64Str
import org.letstrust.model.*
import org.letstrust.services.did.DidService
import org.letstrust.services.jwt.JwtService
import org.letstrust.services.vc.VCService
import java.util.*


object EssifClient {

    private val log = KotlinLogging.logger {}
    private val credentialService = VCService.getService()

    val did: String = DidService.create(DidMethod.ebsi) // Client DID

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
    val ephPrivKey: OctetKeyPair by lazy {
        OctetKeyPairGenerator(Curve.X25519)
            .keyID(JwtService.keyId)
            .generate()
    }

    /**
     * Runs DID + VC Auth (optional)
     * Used for presenting as well as receiving VCs
     */
    fun authenticate() {

        log.debug { "CLIENT::authenticate()" }

        val oidcReq = TrustedIssuerClient.generateAuthenticationRequest()
        log.debug { "Authentication request: $oidcReq" }

        val didAuthReq = validateAuthenticationRequest(oidcReq)
        log.debug { "Authentication request: $didAuthReq" }

        val authResp = this.generateAuthenticationResponse(didAuthReq)
        log.debug { "Authentication response: $authResp" }

        val encAccessToken = TrustedIssuerClient.openSession(authResp)
        log.debug { "Received encrypted access token: $encAccessToken" }

        val accessToken = decryptAccessToken(encAccessToken)
        log.debug { "Received access token for fetching credential: $accessToken" }

        log.debug { "CLIENT::authenticate() - completed" }
    }

    fun decryptAccessToken(encAccessToken: String): String {

        val jwt = EncryptedJWT.parse(encAccessToken)
        //TODO load enc-key based on: jwt.header.keyID
        jwt.decrypt(X25519Decrypter(ephPrivKey))
        return jwt.payload.toString()
    }

    fun validateAuthenticationRequest(authReq: String): DidAuthRequest {

        log.debug { "CLIENT::validateAuthenticationRequest()" }

        val oidcReq = Json.decodeFromString<OidcRequest>(authReq)

        val didAuthReq = OidcUtil.validateOidcAuthenticationRequest(oidcReq)

        // TODO: consider further validation steps

        return didAuthReq
    }

    fun generateAuthenticationResponse(authReq: DidAuthRequest): String {

        log.debug { "CLIENT::generateAuthenticationResponse()" }

        val vc = "" // TODO load VC
        val verifiedClaims = "" // TODO: build VP createVerifiedClaims(didClient, vc)

        return OidcUtil.generateOidcAuthenticationResponse(ephPrivKey.toPublicJWK(), did, verifiedClaims, authReq.nonce)
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
        val vp = credentialService.sign(did, vpReq.encode(), null, null, authKeyId, "assertionMethod")

        log.debug { "Verifiable Presentation generated:\n$vp" }

        //  verifiablePresentationFile.writeText(vp)

        val vpCan = canonicalize(vp)

        return encBase64Str(vpCan)
    }
}
