package org.letstrust

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import mu.KotlinLogging
import org.letstrust.model.*
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets.UTF_8

private val log = KotlinLogging.logger {}

object EssifService {

    val ESSIF_BASE_URL = "https://api.letstrust.org/essif"

    // https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?spaceKey=BLOCKCHAININT&title=2.+Authorization+API
    // https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?spaceKey=BLOCKCHAININT&title=Authorisation+API
    fun authenticate() {
        // request SIOP Authorization Request
        log.info("Request an access request token from the Authorisation API")
        var authReq = this.authenticationRequest()

        println(authReq)

        // process Authorization Request
        log.info("Validating the authentication request (validate the issuer, content, expiration date, etc.)")

        validateAuthenticationRequest(authReq)

        // Verify DID
        log.info("Resolving issuer DID")

        // Establish SIOP Session
        log.info("Assembling authorization response to open a SIOP session")
        val atr = this.siopSessionsRequest(authReq)

        // process ID Token including VP
        log.info("ID Token received")

        log.info("Validating ID Token")

        // Access protected resource
        log.info("Accessing protected EBSI resource ...")
    }

    private fun siopSessionsRequest(authReq: AuthenticationRequestPayload): AccessTokenResponse? {

        val verifiableAuthorization = File("src/test/resources/ebsi/verifiable-authorization2.json").readText()

        val vp = CredentialService.present(verifiableAuthorization, "api.ebsi.xyz", null)

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

        println(Json { prettyPrint = true }.encodeToString(arp))

        val id_token = JwtService.sign("did:key:z6MksTeZpzyCdeRHuvk6kAAfQQCas3NPTRtxnB5a68mDrps5", Json.encodeToString(arp))

        val siopSessionRequest = SiopSessionRequest(id_token)
        println(Json { prettyPrint = true }.encodeToString(siopSessionRequest))


//        {
//            "grantType": "client_credentials",
//            "clientAssertionType": "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
//            "clientAssertion": "eyJhbGciOiJIUzI...",
//            "scope": "openid did_authn"
//        }

        // https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?spaceKey=BLOCKCHAININT&title=Authorisation+API#AuthorisationAPI-AccessTokenandAuthenticatedKeyExchangedatamodels
        // TODO send siopSessionRequest

        // AccessToken Received

        val atr = AccessTokenResponse(
            Ake1EncPayload("JWS encoded access token", "DID of the RP"),
            Ake1JwsDetached("Nonce from the ID Token used for authentication", "ake1_enc_payload", "DID of the Client"),
            "did"
        )

        println("AccessTokenResponse received: $atr")

        return atr // AccessTokenPayload
    }

    fun validateAuthenticationRequest(authReq: AuthenticationRequestPayload) {

        log.debug { "Validating Authentication Request $authReq" }

        if (authReq.claims.id_token.verified_claims.verification.trust_framework != "EBSI") {
            throw Exception("Trustframework needs to be: EBSI")
        }

        //TODO add further validations and validation based on the JSON schema

    }


    // Request parsing and signature validation
    fun authenticationRequest(): AuthenticationRequestPayload {

        val authenticationRequest = "{\n" +
                "  \"scope\": \"ebsi user profile\"\n" +
                "}"

        log.debug { "POST /authentication-requests:\n${authenticationRequest}" }

        val authenticationRequestResponse = "{\n" +
                "  \"uri\": \"openid://?response_type=id_token&client_id=https%3A%2F%2Fapi.ebsi.zyz%2Faccess-tokens&scope=openid%20did_authn&request=eyJraWQiOiJMZXRzVHJ1c3QtS2V5LTBhNzBjZmZlMmQxMDQyY2Q4NDkwYzIxYjcxYjkzZTM3IiwiYWxnIjoiRVMyNTZLIn0.eyJzY29wZSI6Im9wZW5pZCBkaWRfYXV0aG4iLCJpc3MiOiJkaWQ6ZWJzaToweDQxNmU2ZTYxNjI2NTZjMmU0YzY1NjUyZTQ1MmQ0MTJkNTA2ZjY1MmUiLCJjbGFpbXMiOnsiaWRfdG9rZW4iOnsidmVyaWZpZWRfY2xhaW1zIjp7InZlcmlmaWNhdGlvbiI6eyJldmlkZW5jZSI6eyJkb2N1bWVudCI6eyJjcmVkZW50aWFsU2NoZW1hIjp7ImlkIjp7InZhbHVlIjoiaHR0cHM6XC9cL2Vic2kueHl6XC90cnVzdGVkLXNjaGVtYXMtcmVnaXN0cnlcL3ZlcmlmaWFibGUtYXV0aG9yaXNhdGlvbiIsImVzc2VudGlhbCI6dHJ1ZX19LCJ0eXBlIjp7InZhbHVlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiVmVyaWZpYWJsZUF1dGhvcmlzYXRpb24iXSwiZXNzZW50aWFsIjp0cnVlfX0sInR5cGUiOnsidmFsdWUiOiJ2ZXJpZmlhYmxlX2NyZWRlbnRpYWwifX0sInRydXN0X2ZyYW1ld29yayI6IkVCU0kifX19fSwicmVzcG9uc2VfdHlwZSI6ImlkX3Rva2VuIiwicmVnaXN0cmF0aW9uIjoiPHJlZ2lzdHJhdGluIG9iamVjdD4iLCJub25jZSI6IjxyYW5kb20tbm9uY2U-IiwiY2xpZW50X2lkIjoiPHJlZGlyZWN0LXVyaT4ifQ.4SM7quGYTHq8b8jXcx1tQHUay9MZwM4obVN459HMXX3V6lfhGjBeqVQOd3TyE18ORVn8SAviTBLSnkWdZN14zg\"\n" +
                "}"

        // https://github.com/Baeldung/kotlin-tutorials/tree/master/kotlin-libraries-http/src/main/kotlin/com/baeldung/fuel
        //val resp = post("$ESSIF_BASE_URL/authentication-requests", json = mapOf("scope" to "ebsi user profile"))

        log.info("Authorization request received")
        log.debug { "Response of /authentication-requests:\n$authenticationRequestResponse" }

        val oidcReqUri = jsonToOidcAuthenticationRequestUri(authenticationRequestResponse)

        log.debug { "OidcReqUri: $oidcReqUri" }

        if (!JwtService.verify(oidcReqUri.request)) {
            log.error { "Could not verify Authentication Request Token signature: " + oidcReqUri.request }
            throw Exception("Could not verify Authentication Request Token signature: " + oidcReqUri.request)
        } else {
            log.debug { "Authentication Request Token Verified successfully" }
        }

        val claims = JwtService.parseClaims(oidcReqUri.request)

        println(claims)

        println(claims?.get("claims")!!.toString())

        val claim = Json.decodeFromString<Claim>(claims["claims"].toString())

        return AuthenticationRequestPayload(
            claims["scope"].toString(),
            claims["iss"].toString(),
            claims["response_type"].toString(),
            claims["client_id"].toString(),
            claims["nonce"].toString(),
            claims["registration"].toString(),
            claim
        )
    }

    //        {
    //            "uri": "openid://?response_type=id_token&client_id=https%3A%2F%2Fapi.ebsi.zyz%2Faccess-tokens&scope=openid%20did_authn&request=eyJhbGciOiJIUzI1Ni..."
    //        }
    fun jsonToOidcAuthenticationRequestUri(authenticationRequestResponseJson: String): OidcAuthenticationRequestUri {
        try {
            val uri = Json.parseToJsonElement(authenticationRequestResponseJson).jsonObject["uri"].toString()
            val paramString = uri.substringAfter("openid://?")
            val pm = toParamMap(paramString)
            return OidcAuthenticationRequestUri(pm["response_type"]!!, pm["scope"]!!, pm["request"]!!)
        } catch (e: Exception) {
            log.error { "Could not parse $authenticationRequestResponseJson" }
            throw e
        }
    }

    private fun toParamMap(paramString: String): Map<String, String> {
        val paramMap = HashMap<String, String>()
        var pairs = paramString.split("&")

        pairs.forEach { paramMap.put(it.substringBefore("="), URLDecoder.decode(it.substringAfter("="), UTF_8)) }
        return paramMap
    }

}
