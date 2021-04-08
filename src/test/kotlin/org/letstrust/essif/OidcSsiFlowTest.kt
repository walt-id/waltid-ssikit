package org.letstrust.essif


import org.junit.Test
import org.letstrust.services.essif.EnterpriseWalletService
import org.letstrust.services.essif.UserWalletService
import kotlin.test.assertTrue

// https://ec.europa.eu/cefdigital/wiki/display/BLOCKCHAININT/2.+OIDC-SSI+Flow
class OidcSsiFlowTest {
    @Test
    fun runOidcSsiFlowTest() {
        println("1. Request access")
        val ret = UserAgent().requestAccess()
        assertTrue(ret, "OIDC flow returned an failure")
        println("17. Process done successfully")
    }
}

class UserAgent() {
    var rp = RelyingParty()

    fun requestAccess(): Boolean {
        println("2. Sign-On")
        var authReq = rp.signOn()

        println("6. Process Authentication Request: 302 openid://")
        println("7. Process Authentication Request: openid://")
        val authResp = UserWalletService.processAuthenticationRequest(authReq)
        println("11. Authentication Response: Callback /callback 302")
        println("17. Success")
        return rp.callback(authResp)
    }
}

class RelyingParty() {
    fun signOn(): String {
        val authReq = EnterpriseWalletService.auth()

        println("5. Return Authentication Request")

        return authReq
    }

    fun callback(authResp: String): Boolean {
        println("12. Authentication Response: Callback /callback")
        return EnterpriseWalletService.token(authResp)
        print("15. Success")
    }
}


