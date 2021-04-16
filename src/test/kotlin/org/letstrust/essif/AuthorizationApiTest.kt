package org.letstrust.essif

import org.junit.Test
import org.letstrust.services.essif.UserWalletService

// https://ec.europa.eu/cefdigital/wiki/display/BLOCKCHAININT/2.+Authorization+API
class AuthorizationApiTest {

    @Test
    fun testAuthApiFlow() {

        println("--------------------------------------------------------------------------------")
        // Requesting Verifiable Authorization
        println("Accessing protected EBSI resource ...\n")
        UserWalletService.requestAccessToken()


        println("--------------------------------------------------------------------------------")
        // Access protected resource
        println("Accessing protected EBSI resource ...\n")
        UserWalletService.accessProtectedResource(accessToken)
        println("Accessed /protectedResource successfully ✔ ")

    }
}
