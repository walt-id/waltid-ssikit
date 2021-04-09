package org.letstrust.services.essif

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import mu.KotlinLogging
import org.letstrust.model.*
import org.letstrust.services.essif.mock.AuthorizationApi
import org.letstrust.services.jwt.JwtService
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

private val log = KotlinLogging.logger {}

object UserWalletService {

//    val didUrlUser by lazy {
//        DidService.create(DidMethod.web)
//    }

    fun processAuthenticationRequest(authReq: String): String {
        println("8. [UWallet] OIDC Validation")
        println("9. [UWallet] DID AuthN validation")
        println("10. [UWallet] Generate Authentication Response")
        val authResp = ""
        return authResp
    }

    fun validateDidAuthRequest(didAuthRequest: String) {
        println("10. [UWallet] Validate request")

        this.generateDidAuthResponse(didAuthRequest)
    }

    fun validateVcExchangeRequest(vcExchangeRequest: String) {
        println("10. [UWallet] Validate request")

        this.generateDidAuthResponse(vcExchangeRequest)

    }

    fun generateDidAuthResponse(didAuthRequest: String) {
        println("13/11. [UWallet] Generate (DID-)Auth Response")
        println("14/12. [UWallet] /callback (DID-)Auth Response")
        val vcToken = EnterpriseWalletService.validateDidAuthResponse("didAuthResp")
        println("16/14. [UWallet] 200 OK")

    }

    // https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?spaceKey=BLOCKCHAININT&title=2.+Authorization+API
    fun requestVerifiableAuthorization() {
        println("--------------------------------------------------------------------------------")
        println("1 [UWallet] [POST] /authentication-request - Request & validate SIOP AuthorizationRequest (init DID auth) ...")
        val authReq = didAuthAuthorizationApi()
        // processing and validating Authorization Request
        validateAuthenticationRequest(authReq)

        println("--------------------------------------------------------------------------------")
        println("Establish SIOP session (finalize DID auth) ...")
        val atr = this.siopSessionsRequest(authReq)

        // AKE Protocol
        validateAccessTokenResponse(atr)
    }

    fun accessProtectedResource() {

    }



    private fun didAuthAuthorizationApi(): AuthenticationRequestPayload {
        val authenticationRequest = Json.encodeToString(mapOf("scope" to "ebsi user profile"))

        println("Request an access request token from the Authorisation API (POST /authentication-requests):\n${authenticationRequest}\n")

        val authenticationRequestResponse = AuthorizationApi.getAuthorizationRequest(authenticationRequest)


        // https://github.com/Baeldung/kotlin-tutorials/tree/master/kotlin-libraries-http/src/main/kotlin/com/baeldung/fuel
        //val resp = post("$ESSIF_BASE_URL/authentication-requests", json = mapOf("scope" to "ebsi user profile"))

        println("3. [UWallet] 200 <Authorization Request> received. (response of /authentication-requests):\n$authenticationRequestResponse")

        val oidcReqUri = jsonToOidcAuthenticationRequestUri(authenticationRequestResponse)

        log.debug { "OidcReqUri: $oidcReqUri" }

        if (false){//!JwtService.verify(oidcReqUri.request)) {
            log.error { "Could not verify Authentication Request Token signature: " + oidcReqUri.request }
            throw Exception("Could not verify Authentication Request Token signature: " + oidcReqUri.request)
        } else {
            println("\nJWT signature of Authentication Request Token Verified successfully ✔\n")
        }

        val claims = JwtService.parseClaims(oidcReqUri.request)!!

        // println(claims?.get("claims")!!.toString())

        val claim = Json.decodeFromString<Claim>(claims["claims"].toString())

        val arp = AuthenticationRequestPayload(
            claims["scope"].toString(),
            claims["iss"].toString(),
            claims["response_type"].toString(),
            claims["client_id"].toString(),
            claims["nonce"].toString(),
            claims["registration"].toString(),
            claim
        )

        println("Decoded Authorization Request:\n" + Json { prettyPrint = true }.encodeToString(arp) + "\n")
        return arp
    }

    private fun jsonToOidcAuthenticationRequestUri(authenticationRequestResponseJson: String): OidcAuthenticationRequestUri {
        try {
            val uri = Json.parseToJsonElement(authenticationRequestResponseJson).jsonObject["uri"].toString()
            val paramString = uri.substringAfter("openid://?")
            val pm = toParamMap(paramString)
            return OidcAuthenticationRequestUri(pm["response_type"]!!, pm["scope"]!!, pm["request"]!!)
        } catch (e: Exception) {
            log.error { "Could not parse AuthenticationRequestResponse: $authenticationRequestResponseJson" }
            throw e
        }
    }

    private fun toParamMap(paramString: String): Map<String, String> {
        val paramMap = HashMap<String, String>()
        val pairs = paramString.split("&")

        pairs.forEach { paramMap[it.substringBefore("=")] = URLDecoder.decode(it.substringAfter("="), StandardCharsets.UTF_8) }
        return paramMap
    }


    private fun validateAuthenticationRequest(authReq: AuthenticationRequestPayload) {

        log.debug { "Validating Authentication Request $authReq" }

        if (authReq.claims.id_token.verified_claims.verification.trust_framework != "EBSI") {
            throw Exception("Trust framework needs to be: EBSI")
        }

        //TODO add further validations and validation based on the JSON schema

        println("Validating Authentication request:")
        println("- json schema: ✔")
        println("- valid issuer: ✔")
        println("- expiration date: ✔")
        println("- EBSI Trust Framework: ✔")
        println("")
    }

    private fun siopSessionsRequest(authReq: AuthenticationRequestPayload): AccessTokenResponse? {

        val verifiableAuthorization = File("src/test/resources/ebsi/verifiable-authorization.json").readText()

        println("Loading Verifiable Authorization:\n$verifiableAuthorization\n")

        val vp = "" //CredentialService.present(verifiableAuthorization, "api.ebsi.xyz", null)

        // TODO: set correct values
        val arp = AuthenticationResponsePayload(
            "did:ebsi:0x123abc",
            "thumbprint of the sub_jwk",
            "did:ebsi:RP-did-here",
            1610714000,
            1610714900,
            "signing JWK",
            "did:ebsi:0x123abc#authentication-key-proof-3",
            authReq.nonce,
            AuthenticationResponseVerifiedClaims(vp, "enc_key")
        )

        println("AuthenticationResponse assembled:\n" + Json { prettyPrint = true }.encodeToString(arp) + "\n")

        val signingKey = "did:key:z6MksTeZpzyCdeRHuvk6kAAfQQCas3NPTRtxnB5a68mDrps5"

        println("Signing AuthenticationResponse with key: $signingKey\n")
        val idToken =
            "yCdeRHuvk6kAAfQQCz6MksTeZpzyCdeRHuvk6kAAfQQCas3NPTRtxnB5a68mDrps5as3NPTRtxnB5a68mDrps5ZpzyCdeRHuz6MksTeZpzyCdeRHuvk6kAAfQQCas3NPTRtxnB5a68mDrps5vk6kAAfQQCas3NPTRtxnB5a68mDrps5eZpzyCdeRHuz6MksTeZpzyCdeRHuvk6kAAfQQCas3NPTRtxnB5a68mDrps5vk6kAAfQQCas3NPTRtxnB5a68mDrps5"//JwtService.sign(sigingKey, Json.encodeToString(arp))

        val siopSessionRequest = SiopSessionRequest(idToken)
        println("SIOP Session Request:\n" + Json { prettyPrint = true }.encodeToString(siopSessionRequest) + "\n")


        /*
        {
        "grantType": "client_credentials",
        "clientAssertionType": "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
        "clientAssertion": "eyJhbGciOiJIUzI...",
        "scope": "openid did_authn"
        }
        https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?spaceKey=BLOCKCHAININT&title=Authorisation+API#AuthorisationAPI-AccessTokenandAuthenticatedKeyExchangedatamodels
        TODO send siopSessionRequest
        AccessToken Received
        //        val atr = AccessTokenResponse(
        //            Ake1EncPayload("JWS encoded access token", "DID of the RP"),
        //            Ake1JwsDetached("Nonce from the ID Token used for authentication", "ake1_enc_payload", "DID of the Client"),
        //            "did"
        )
        */
        //Token payload validation https://openid.net/specs/openid-connect-core-1_0.html#CodeIDToken

        val atr = null
        // println("AccessTokenResponse received:\n" + Json { prettyPrint = true }.encodeToString(atr))


        return atr // AccessTokenPayload
    }

    // https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?pageId=271909906
    private fun validateAccessTokenResponse(atr: AccessTokenResponse?) {
        log.debug { "Validating Access Token Response $atr" }

        //TODO validate access token

        println("Validating AccessToken response:")
        println("- JWT signature: ✔")
        println("- DID of RP: ✔")
        println("- DID of Client: ✔")
        println("- ake1_nonce: ✔")
        println("")
    }


}
