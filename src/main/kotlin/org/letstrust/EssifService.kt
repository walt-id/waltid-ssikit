package org.letstrust

import khttp.post
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

object EssifService {

    // https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?spaceKey=BLOCKCHAININT&title=Authorisation+API
    fun authenticate() {
        // request SIOP Authorization Request
        log.info("Request an access request token from the Authorisation API")
        log.info("Authorization request received")
        // process Authorization Request
        log.info("Validating the authentication request (validate the issuer, content, expiration date, etc.)")

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

    // https://github.com/Baeldung/kotlin-tutorials/tree/master/kotlin-libraries-http/src/main/kotlin/com/baeldung/fuel
    fun authenticationRequest() {

        val token = post("https://api.letstrust.io/users/auth/login", json = mapOf("email" to "philipp.potisk@omnecon.com", "password" to "philtest")).jsonObject["token"]
        println(token)

    }

}
