package org.letstrust.essif

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.letstrust.common.readEssif
import org.letstrust.crypto.encBase64
import org.letstrust.model.*
import org.letstrust.services.essif.*
import org.letstrust.services.key.KeyService
import java.net.URLEncoder
import java.util.*

class VcIssuanceFlowTest {


    @Test
    fun generateDidAuthRequest() {
        // println(EnterpriseWalletService.generateDidAuthRequest())

        val kid = "22df3f6e54494c12bfb559e171cfe747"
        val client_id = "http://localhost:8080/redirect" // redirect url
        val scope = "openid did_authn"
        val response_type = "id_token"
        val publicKeyJwk = Json.decodeFromString<Jwk>(KeyService.toJwk(kid).toPublicJWK().toString())
        val authRequestHeader = AuthenticationHeader("ES256K", "JWT", publicKeyJwk)
        val iss = "did:ebsi:0x416e6e6162656c2e4c65652e452d412d506f652e"
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
        val callback = "https://ti.example.com/callback"
        val didAuthReq = DidAuthRequest(response_type, client_id, scope, nonce, didAuthRequestJwt, callback)

        val didAuthOidcReq = toOidcRequest(didAuthReq)

        println(Json.encodeToString(didAuthOidcReq))
    }

    private fun toOidcRequest(didAuthReq: DidAuthRequest): OidcRequest {
        val authRequestJwt = encBase64(Json.encodeToString(didAuthReq).toByteArray())

        val clientId = URLEncoder.encode(didAuthReq.client_id)
        val scope = URLEncoder.encode(didAuthReq.scope)

        val uri = "openid://?response_type=id_token&client_id=$clientId&scope=$scope&request=$authRequestJwt"
        return OidcRequest(uri, didAuthReq.callback)
    }

    @Test
    fun generateDidAuthResponse() {
        val kid = "22df3f6e54494c12bfb559e171cfe747"
        val publicKeyJwk = Json.decodeFromString<Jwk>(KeyService.toJwk(kid).toPublicJWK().toString())
        val nonce = "asdf"
        val vp = "{\n" +
                "  \"@context\": [\n" +
                "    \"https://www.w3.org/2018/credentials/v1\"\n" +
                "  ],\n" +
                "  \"type\": [\n" +
                "    \"VerifiablePresentation\"\n" +
                "  ],\n" +
                "  \"verifiableCredential\": [<empty>],\n" +
                "  \"holder\": \"{client DID}\",\n" +
                "  \"proof\": {\n" +
                "    \"type\": \"Ed25519Signature2018\",\n" +
                "    \"created\": \"2020-04-26T21:24:11Z\",\n" +
                "    \"domain\": \"api.ebsi.xyz\",\n" +
                "    \"jws\": \"eyJhbGciOiJFZERTQSIl19..53JU-HrqnrTM46mcopmeSQ016lw\",\n" +
                "    \"proofPurpose\": \"authentication\",\n" +
                "    \"verificationMethod\": \"{client DID Key}\"\n" +
                "  }\n" +
                "}"
        val authHeader = AuthenticationHeader("ES256K", "JWT", publicKeyJwk)

        val authRespPayload = AuthenticationResponsePayload(
            "did:ebsi:0x123abc",
            "thumbprint of the sub_jwk",
            "did:ebsi:RP-did-here",
            1610714000,
            1610714900,
            "signing JWK",
            "did:ebsi:0x123abc#authentication-key-proof-3",
            nonce,
            AuthenticationResponseVerifiedClaims(vp, "enc_key")
        )

        val authRespJwt = AuthenticationResponseJwt(authHeader, authRespPayload)

        val authRespJwtEnc = encBase64(Json.encodeToString(authRespJwt).toByteArray())

        println(authRespJwtEnc)
    }

    @Test
    fun validateDidAuthRequest() {
        val didAuthReq = readEssif("onboarding-did-ownership-req")

        // TODO validate data structure

        println(didAuthReq)
    }

    @Test
    fun testVcIssuanceFlow() {
        EssifFlowRunner.vcIssuance()
    }
}
