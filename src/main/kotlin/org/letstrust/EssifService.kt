package org.letstrust

import khttp.get
import khttp.post
import mu.KotlinLogging
import org.json.JSONObject

private val log = KotlinLogging.logger {}

object EssifService {

    fun authenticate() {
        // request SIOP Authorization Request
        authenticationRequest()
        // process Authorization Request

        // Verify DID

        // Establish SIOP Session

        // process ID Token including VP

        // Access protected resource
    }

    // https://github.com/Baeldung/kotlin-tutorials/tree/master/kotlin-libraries-http/src/main/kotlin/com/baeldung/fuel
    fun authenticationRequest() {

        val token = post("https://api.letstrust.io/users/auth/login", json = mapOf("email" to "philipp.potisk@omnecon.com", "password" to "philtest")).jsonObject["token"]
        println(token)

    }

}
