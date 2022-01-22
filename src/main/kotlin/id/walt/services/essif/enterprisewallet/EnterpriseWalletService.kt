package id.walt.services.essif.enterprisewallet

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import id.walt.common.readEssif
import id.walt.common.toParamMap
import id.walt.model.AuthRequestResponse
import id.walt.model.AuthenticationRequestJwt
import id.walt.model.DidAuthRequest
import id.walt.servicematrix.BaseService
import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.services.did.DidService
import id.walt.services.essif.TrustedIssuerClient
import id.walt.services.essif.userwallet.UserWalletService
import id.walt.services.jwt.JwtService
import id.walt.services.key.KeyService
import mu.KotlinLogging
import java.time.Instant
import java.util.*

open class EnterpriseWalletService : WaltIdService() {

    override val implementation get() = serviceImplementation<EnterpriseWalletService>()

    private val log = KotlinLogging.logger {}
    private val keyService = KeyService.getService()
    private val jwtService = JwtService.getService()

    fun constructAuthResponseJwt(did: String, redirectUri: String, nonce: String): String {

        //val kid = "$did#key-1"
        val kid = DidService.load(did).authentication!![0]
        val key = keyService.toJwk(did, jwkKeyId = kid)
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

        val jwt = jwtService.sign(kid, payload)

        log.debug { "JWT: $jwt" }

        val jwtToVerify = SignedJWT.parse(jwt)
        log.debug { jwtToVerify.header }
        log.debug { jwtToVerify.payload }

        jwtService.verify(jwt).let { if (!it) throw IllegalStateException("Generated JWK not valid") }

        log.debug { "AuthResponse JWT: $jwt" }
        return jwt
    }

    fun parseDidAuthRequest(authResp: AuthRequestResponse): DidAuthRequest {
        val paramString = authResp.session_token.substringAfter("openid://?")
        val pm = toParamMap(paramString)
        // TODO parse AuthReqJwt
        val req = pm["request"]!!
        val authReqJqt: AuthenticationRequestJwt? = null

        return DidAuthRequest(
            pm["response_type"]!!,
            pm["client_id"]!!,
            pm["scope"]!!,
            pm["nonce"]!!,
            authReqJqt,
            "callback"
        )
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
        val didOwnershipReq = TrustedIssuerClient.onboards()
        log.debug { "didOwnershipReq: $didOwnershipReq" }
        log.debug { "didOwnershipReqHeader:" + readEssif("onboarding-onboards-resp-header") }
        log.debug { "didOwnershipReqBody: " + readEssif("onboarding-onboards-resp-body") }
        println("7. [EWallet] Signed Challenge")

        val signedChallenge = readEssif("onboarding-onboards-callback-req")
        log.debug { "signedChallenge: $signedChallenge" }
        val verifiableAuthorization = TrustedIssuerClient.signedChallenge(signedChallenge)
        println("12. [EWallet] 201 V. Authorization")
        return verifiableAuthorization
    }

    fun requestVerifiableCredential(): String {
        val didOwnershipReq = TrustedIssuerClient.requestVerifiableCredential()
        log.debug { didOwnershipReq }
        println("5. [EWallet] Request DID prove")
        return didOwnershipReq
    }

    fun getVerifiableCredential(didOwnershipReq: String, didOfLegalEntity: String): String {
        // TODO Build didOwnershipResp
        val didOwnershipResp = readEssif("onboarding-did-ownership-resp")
        val vIdRequest = TrustedIssuerClient.didOwnershipResponse(didOwnershipResp)
        log.debug { "vIdRequest: $vIdRequest" }
        val vId = TrustedIssuerClient.getCredential("id")
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

    fun generateDidAuthRequest(): String {
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


    companion object : ServiceProvider {
        override fun getService() = object : EnterpriseWalletService() {}
        override fun defaultImplementation() = WaltIdEnterpriseWalletService()
    }
}

/*
    val authRequestResponse = AuthRequestResponse("session_token=openid://?response_type=id_token&client_id=https%3A%2F%2Fapi.preprod.ebsi.eu%2Fusers-onboarding%2Fv1%2Fauthentication-responses&scope=openid%20did_authn&nonce=83129e94-06a1-4dd8-8deb-1f5d8149a463&request=eyJhbGciOiJFUzI1NksiLCJ0eXAiOiJKV1QiLCJraWQiOiJodHRwczovL2FwaS5wcmVwcm9kLmVic2kuZXUvdHJ1c3RlZC1hcHBzLXJlZ2lzdHJ5L3YyL2FwcHMvMHgwNmQzNGI0ZWExYzdhYjk1OGE4ZmE3ZmQ1MmE4NzNjNjhmY2Q1ZTFhNjU1YzU0YTQyMDlhMGM1NzVmOGRjMjljIn0.eyJpYXQiOjE2Mjc0ODUyNTksImV4cCI6MTYyNzQ4NTU1OSwiaXNzIjoiZGlkOmVic2k6NGpQeGNpZ3ZmaWZaeVZ3eW01emp4YUtYR0pUdDdZd0Z0cGc2QVh0c1I0ZDUiLCJzY29wZSI6Im9wZW5pZCBkaWRfYXV0aG4iLCJyZXNwb25zZV90eXBlIjoiaWRfdG9rZW4iLCJjbGllbnRfaWQiOiJodHRwczovL2FwaS5wcmVwcm9kLmVic2kuZXUvdXNlcnMtb25ib2FyZGluZy92MS9hdXRoZW50aWNhdGlvbi1yZXNwb25zZXMiLCJub25jZSI6IjgzMTI5ZTk0LTA2YTEtNGRkOC04ZGViLTFmNWQ4MTQ5YTQ2MyJ9.esJ_07M4muhIIGIoHy5kaUGhR_IuGPUMfO6eyBvHiC8gvjzAoOi8B7iCWd6ADaDqhrQpRqQtrtO0KsnZPz8Jew")
    EnterpriseWalletService.getService().parseDidAuthRequest(authRequestResponse)

    val req = "eyJhbGciOiJFUzI1NksiLCJ0eXAiOiJKV1QiLCJraWQiOiJodHRwczovL2FwaS5wcmVwcm9kLmVic2kuZXUvdHJ1c3RlZC1hcHBzLXJlZ2lzdHJ5L3YyL2FwcHMvMHgwNmQzNGI0ZWExYzdhYjk1OGE4ZmE3ZmQ1MmE4NzNjNjhmY2Q1ZTFhNjU1YzU0YTQyMDlhMGM1NzVmOGRjMjljIn0.eyJpYXQiOjE2Mjc0ODUyNTksImV4cCI6MTYyNzQ4NTU1OSwiaXNzIjoiZGlkOmVic2k6NGpQeGNpZ3ZmaWZaeVZ3eW01emp4YUtYR0pUdDdZd0Z0cGc2QVh0c1I0ZDUiLCJzY29wZSI6Im9wZW5pZCBkaWRfYXV0aG4iLCJyZXNwb25zZV90eXBlIjoiaWRfdG9rZW4iLCJjbGllbnRfaWQiOiJodHRwczovL2FwaS5wcmVwcm9kLmVic2kuZXUvdXNlcnMtb25ib2FyZGluZy92MS9hdXRoZW50aWNhdGlvbi1yZXNwb25zZXMiLCJub25jZSI6IjgzMTI5ZTk0LTA2YTEtNGRkOC04ZGViLTFmNWQ4MTQ5YTQ2MyJ9.esJ_07M4muhIIGIoHy5kaUGhR_IuGPUMfO6eyBvHiC8gvjzAoOi8B7iCWd6ADaDqhrQpRqQtrtO0KsnZPz8Jew"

    val authenticationRequestJwt = Json.decodeFromString<AuthenticationRequestJwt>(req)
*/
