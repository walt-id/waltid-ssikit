package org.letstrust.essif


import org.junit.Test
import kotlin.test.assertTrue

// https://ec.europa.eu/cefdigital/wiki/display/BLOCKCHAININT/2.+OIDC-SSI+Flow
class OidcSsiFlowTest {
    @Test
    fun oidcSsiFlowTest() {
        println("1. Request access")
        val ret = UserAgent().requestAccess()
        assertTrue(ret, "OIDC flow returned an failure")
        println("17. Process done successfully")
    }
}




