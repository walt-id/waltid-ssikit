package org.letstrust

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import mu.KotlinLogging
import org.letstrust.model.OidcAuthenticationRequestUri
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets.UTF_8

private val log = KotlinLogging.logger {}

object EssifService {

    val ESSIF_BASE_URL = "https://api.letstrust.org/essif"

    // https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?spaceKey=BLOCKCHAININT&title=Authorisation+API
    fun authenticate() {
        // request SIOP Authorization Request
        log.info("Request an access request token from the Authorisation API")
        this.authenticationRequest()

        log.info("Authorization request received")
        // process Authorization Request
        log.info("Validating the authentication request (validate the issuer, content, expiration date, etc.)")

        validateAuthenticationRequest()

        // Verify DID
        log.info("Resolving issuer DID")

        // Establish SIOP Session
        log.info("Assembling authorization response to open a SIOP session")

        // process ID Token including VP
        log.info("ID Token received")

        log.info("Validating ID Token")

        // Access protected resource
        log.info("Accessing protected EBSI resource ...")
    }

    fun validateAuthenticationRequest() {
        val authenticationRequestStr = File("src/test/resources/ebsi/authentication-request-payload.json").readText()

        println(authenticationRequestStr)


    }

    // https://github.com/Baeldung/kotlin-tutorials/tree/master/kotlin-libraries-http/src/main/kotlin/com/baeldung/fuel
    fun authenticationRequest(): OidcAuthenticationRequestUri {

//        {
//            "scope": "ebsi user profile"
//        }
        val authenticationRequest = "{\n" +
                "  \"scope\": \"ebsi user profile\"\n" +
                "}"

        log.debug { "POST /authentication-requests:\n${authenticationRequest}" }

        val authenticationRequestResponse = "{\n" +
                "  \"uri\": \"openid://?response_type=id_token&client_id=https%3A%2F%2Fapi.ebsi.zyz%2Faccess-tokens&scope=openid%20did_authn&request=eyJhbGciOiJIUzI1Ni...\"\n" +
                "}"

        //val resp = post("$ESSIF_BASE_URL/authentication-requests", json = mapOf("scope" to "ebsi user profile"))

        log.debug { "Response of /authentication-requests:\n$authenticationRequestResponse" }

        val oidcReq = jsonToOidcAuthenticationRequestUri(authenticationRequestResponse)

        log.debug { "SIOP Request: $oidcReq" }

        return oidcReq;
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
