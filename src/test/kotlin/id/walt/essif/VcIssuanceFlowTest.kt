package id.walt.essif

import com.beust.klaxon.Klaxon
import com.nimbusds.jwt.SignedJWT
import id.walt.common.readEssif
import id.walt.common.urlEncode
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.encBase64
import id.walt.model.*
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.ecosystems.essif.EssifClient
import id.walt.services.ecosystems.essif.EssifServer
import id.walt.services.ecosystems.essif.userwallet.UserWalletService
import id.walt.services.key.InMemoryKeyService
import id.walt.services.key.KeyService
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.style.AnnotationSpec
import java.util.*

class VcIssuanceFlowTest : AnnotationSpec() {

    private val keyService = KeyService.getService()

    init {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    }

    private fun generateDidAuthRequest(): String {
        // println(EnterpriseWalletService.generateDidAuthRequest())

        val kid = "22df3f6e54494c12bfb559e171cfe747"
        val client_id = "http://localhost:8080/redirect" // redirect url
        val scope = "openid did_authn"
        val response_type = "id_token"
        val publicKeyJwk = Klaxon().parse<Jwk>(keyService.toJwk(kid).toPublicJWK().toString())!!
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
                listOf()
            )
        )
        val authRequestPayload =
            AuthenticationRequestPayload(scope, iss, response_type, client_id, nonce, registration, claims)
        val didAuthRequestJwt = AuthenticationRequestJwt(authRequestHeader, authRequestPayload)
        val callback = "https://ti.example.com/callback"
        val didAuthReq = DidAuthRequest(response_type, client_id, scope, nonce, didAuthRequestJwt, callback)

        val didAuthOidcReq = toOidcRequest(didAuthReq)

        return Klaxon().toJsonString(didAuthOidcReq)
    }

    private fun toOidcRequest(didAuthReq: DidAuthRequest): OidcRequest {
        val authRequestJwt = encBase64(Klaxon().toJsonString(didAuthReq).toByteArray())

        val clientId = urlEncode(didAuthReq.client_id)
        val scope = urlEncode(didAuthReq.scope)

        val uri =
            "openid://?response_type=id_token&client_id=$clientId&scope=$scope&request=$authRequestJwt&nonce=${didAuthReq.nonce}"
        return OidcRequest(uri, didAuthReq.callback)
    }


    @Test
    fun testOpenSiopSession() {

        val siopRequest = EssifServer.generateAuthenticationRequest()

//        val emphPrivKey = ECKeyGenerator(Curve.SECP256K1)
//            .keyUse(KeyUse.SIGNATURE)
//            .keyID("123")
//            .generate()
        val emphKeyId = InMemoryKeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1)

        val verifiedClaims = "{}"
        val nonce = ""

        val idToken = UserWalletService.constructSiopResponseJwt(emphKeyId, verifiedClaims, nonce)

        print(idToken)
        val bearerToken = ""

        val siopResponse =
            generateSiopSessionResponse(idToken) // LegalEntityClient.eos.siopSession(idToken, bearerToken)

        //val accessTokenResponse = Klaxon().parse<AccessTokenResponse>(siopResponse)

        //println(accessTokenResponse)
    }

    @Test
    fun testGenerateSiopSessionResponse() {
        val token =
            "eyJraWQiOiJkaWQ6ZWJzaToyMlU1UFE3bmt0aURlYTE5TXpRV2VFZkFIOUZIM1cyd2Fyb3FhWXhxaXp0MnJEejcjZGU3NTIzZjc2MWViNDVjM2E2MjcxNzM2ZWNiNzU0ODMiLCJ0eXAiOiJKV1QiLCJhbGciOiJFZERTQSJ9.eyJhdWQiOiJcL3Npb3Atc2Vzc2lvbnMiLCJzdWIiOiJ5NjdwSTZuX1lQQl9vMGNRUTNzQVp3UnBCbE1RaTRPbEtKU1R1QzN1eXhrIiwiaXNzIjoiaHR0cHM6XC9cL3NlbGYtaXNzdWVkLm1lIiwiY2xhaW1zIjp7InZlcmlmaWVkX2NsYWltcyI6Int9IiwiZW5jcnlwdGlvbl9rZXkiOnsia3R5IjoiT0tQIiwiY3J2IjoiWDI1NTE5IiwieCI6IjJlRVZ2M21ZMzJlbmdEVkd5Sk5TQkJ5VXB0WnZBS2docG11OVpJVW5NamsifX0sInN1Yl9qd2siOnsia3R5IjoiT0tQIiwiZCI6Imh5QlF4ZVZYWXJpa2hOZ0J3T0lIeXU5S01wbEpLZkNKNEJMbHZVUG5BbzgiLCJjcnYiOiJYMjU1MTkiLCJraWQiOiIxMjMiLCJ4IjoiMmVFVnYzbVkzMmVuZ0RWR3lKTlNCQnlVcHRadkFLZ2hwbXU5WklVbk1qayJ9LCJleHAiOjE2MjYxMTc5MjQsImlhdCI6MTYyNjExNzYyNCwibm9uY2UiOiIifQ.nnkAf7-1xhsvikO-8W7W4CrFnaPTZHWOnuoAgugJ9cw4SYhlRGbMrQKeg5fiqgKWjYcJUoryFpuQ6h5IBnkQDQ"
        val encToken = generateSiopSessionResponse(token)
        println(encToken)
    }

    fun generateSiopSessionResponse(idToken: String): String {

        val jwt = SignedJWT.parse(idToken)

        val claims = jwt.jwtClaimsSet.getClaim("claims") as Map<String, Any>

        val vp = claims["verified_claims"]
        val encryption_key = claims["encryption_key"] as Map<String, Any>

        // TODO verify VP
        println(vp)

        // Generate Access token
        val accessToken = UUID.randomUUID().toString()

        // Decrypt JWE: See UserWalletService::siopSession
//        val emphClientKey = ECKey.parse(encryption_key)
//
//        val privateKeyId = keyService.generate(KeyAlgorithm.EdDSA_Ed25519).id
//        println(privateKeyId)
//        val siopResponse = jwtService.encrypt(privateKeyId, emphClientKey, accessToken)

        return "siopResponse"
    }

//    @Test
//    fun generateDidAuthResponse() {
//        val kid = "22df3f6e54494c12bfb559e171cfe747"
//        val publicKeyJwk = Klaxon().parse<Jwk>(KeyService.toJwk(kid).toPublicJWK().toString())
//        val nonce = "asdf"
//        val vp = "{\n" +
//                "  \"@context\": [\n" +
//                "    \"https://www.w3.org/2018/credentials/v1\"\n" +
//                "  ],\n" +
//                "  \"type\": [\n" +
//                "    \"VerifiablePresentation\"\n" +
//                "  ],\n" +
//                "  \"verifiableCredential\": [<empty>],\n" +
//                "  \"holder\": \"{client DID}\",\n" +
//                "  \"proof\": {\n" +
//                "    \"type\": \"Ed25519Signature2018\",\n" +
//                "    \"created\": \"2020-04-26T21:24:11Z\",\n" +
//                "    \"domain\": \"api.ebsi.xyz\",\n" +
//                "    \"jws\": \"eyJhbGciOiJFZERTQSIl19..53JU-HrqnrTM46mcopmeSQ016lw\",\n" +
//                "    \"proofPurpose\": \"authentication\",\n" +
//                "    \"verificationMethod\": \"{client DID Key}\"\n" +
//                "  }\n" +
//                "}"
//        val authHeader = AuthenticationHeader("ES256K", "JWT", publicKeyJwk)
//
//        val authRespPayload = AuthenticationResponsePayload(
//            "did:ebsi:0x123abc",
//            "thumbprint of the sub_jwk",
//            "did:ebsi:RP-did-here",
//            1610714000,
//            1610714900,
//            "signing JWK",
//            "did:ebsi:0x123abc#authentication-key-proof-3",
//            nonce,
//            AuthenticationResponseVerifiedClaims(vp, "enc_key")
//        )
//
//        val authRespJwt = AuthenticationResponseJwt(authHeader, authRespPayload)
//
//        val authRespJwtEnc = encBase64(Klaxon().toJsonString(authRespJwt).toByteArray())
//
//        println(authRespJwtEnc)
//    }

    @Test
    fun validateDidAuthRequest() {
        val didAuthReq = readEssif("onboarding-did-ownership-req")

        // TODO validate data structure

        println(didAuthReq)
    }

    @Test
    fun testVcIssuanceFlow() {
        EssifClient.vcIssuance()
    }
}
