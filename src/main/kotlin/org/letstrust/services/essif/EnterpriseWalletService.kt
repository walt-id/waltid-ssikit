package org.letstrust.services.essif

import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.letstrust.common.readEssif
import org.letstrust.common.toParamMap
import org.letstrust.model.AuthRequestResponse
import org.letstrust.model.DidAuthRequest
import org.letstrust.services.did.DidService
import org.letstrust.services.essif.mock.DidRegistry
import org.letstrust.services.jwt.JwtService
import org.letstrust.services.key.KeyService
import java.time.Instant
import java.util.*

object EnterpriseWalletService {

    private val log = KotlinLogging.logger {}

//    val didUrlRp by lazy {
//        DidService.create(DidMethod.web)
//    }


    fun constructAuthResponseJwt(did: String, redirectUri: String, nonce: String): String {

        //val kid = "$did#key-1"
        val kid = DidService.loadDidEbsi(did).authentication!![0]
        val key = KeyService.toJwk(did, false, kid)
        val thumbprint = key.computeThumbprint().toString()

        val payload = JWTClaimsSet.Builder()
            .issuer("https://self-issued.me")
            .audience(redirectUri)
            .subject(thumbprint)
            .issueTime(Date.from(Instant.now()))
            .expirationTime(Date.from(Instant.now().plusSeconds(300)))
            .claim("nonce", nonce)
            .claim("sub_jwk", key.toJSONObject())
            .build().toString()

        val jwt = JwtService.sign(kid, payload)

        log.debug { "JWT: $jwt" }

        val jwtToVerify = SignedJWT.parse(jwt)
        log.debug { jwtToVerify.header }
        log.debug { jwtToVerify.payload }

        JwtService.verify(jwt).let { if (!it) throw IllegalStateException("Generated JWK not valid") }

        val authResponseJwt = "$redirectUri#id_token=$jwt"
        log.debug { "AuthResponse JWT: $authResponseJwt" }
        return authResponseJwt
    }

    fun parseDidAuthRequest(authResp: AuthRequestResponse): DidAuthRequest {
        val paramString = authResp.session_token.substringAfter("openid://?")
        val pm = toParamMap(paramString)
        return DidAuthRequest(pm["response_type"]!!, pm["client_id"]!!, pm["scope"]!!, pm["nonce"]!!, Json.decodeFromString(pm["request"]!!), "callback")
    }

    // TODO consider the following stubs

    fun createDid(): String {
        return UserWalletService.createDid()
    }

    // https://besu.hyperledger.org/en/stable/HowTo/Send-Transactions/Account-Management/
    fun didGeneration(): String {
        println("1. [EWallet] Generate ETH address (keys)")
        println("2. [EWallet] Generate DID Controlling Keys)")
        println("3. [EWallet] Store DID Controlling Private Key")
        println("4. [EWallet] Generate DID Document")
        return "did"
    }

    fun requestVerifiableAuthorization(did: String): String {
        println("5. [EWallet] POST /onboards")
        val didOwnershipReq = LegalEntityClient.eos.onboards()
        log.debug { "didOwnershipReq: $didOwnershipReq" }
        log.debug { "didOwnershipReqHeader:" + readEssif("onboarding-onboards-resp-header") }
        log.debug { "didOwnershipReqBody: " + readEssif("onboarding-onboards-resp-body") }
        println("7. [EWallet] Signed Challenge")

        val signedChallenge = readEssif("onboarding-onboards-callback-req")
        log.debug { "signedChallenge: $signedChallenge" }
        val verifiableAuthorization = LegalEntityClient.eos.signedChallenge(signedChallenge)
        println("12. [EWallet] 201 V. Authorization")
        return verifiableAuthorization
    }

    fun requestVerifiableCredential(): String {
        val didOwnershipReq = LegalEntityClient.eos.requestVerifiableCredential()
        log.debug { didOwnershipReq }
        println("5. [EWallet] Request DID prove")
        return didOwnershipReq
    }

    fun getVerifiableCredential(didOwnershipReq: String, didOfLegalEntity: String): String {
        // TODO Build didOwnershipResp
        val didOwnershipResp = readEssif("onboarding-did-ownership-resp")
        val vIdRequest = LegalEntityClient.eos.didOwnershipResponse(didOwnershipResp)
        log.debug { "vIdRequest: $vIdRequest" }
        val vId = LegalEntityClient.eos.getCredential("id")
        println("13 [EWallet] 200 <V.ID>")
        return vId
    }

    fun generateOidcAuthRequest(): String {
        println("3/2. [EWallet] Auth /auth")

        println("4/3. [EWallet] Generate Authentication Request")
        val authRequest = "openid://?response_type=id_token\n" +
                "    &client_id=https%3A%2F%2Frp.example.com%2Fcb\n" +
                "    &scope=openid%20did_authn\n" +
                "    &request=<authentication-request-JWS>"
        return authRequest
    }

    fun generateDidAuthRequest() : String {
        println("3. [EWallet] Generate <DID-Auth Request>")
        return readEssif("onboarding-did-ownership-req")
    }

    fun token(oidcAuthResp: String): Boolean {
        println("13. [EWallet] /token <Authentication Response>")

        println("14. [EWallet] OIDC Validation")

        println("15. [EWallet] DID AuthN validation")
        return true
    }

    fun validateDidAuthResponse(didAuthResp: String): String {
        println("15/13. [EWallet]  Validate response")
        return "vcToken"
    }

    fun getSession(sessionId: String): String {
        println("7/16. [EWallet] /sessions/{id}")
        println("8/17. [EWallet] 428 (no content)")
        return "notfound - or session"
    }

    // used in Trusted Issuer Onboarding
    fun onboardTrustedIssuer(scanQrUri: String) {
        // Send information to the Trusted Accreditation Organization
        println("9. [EWallet] GET /sessions/{id}")
        println("10. [EWallet] 200 <Sessions>")
        println("11. [EWallet] [POST] /sessions")
    }


}
