package org.letstrust.essif


import org.junit.Test
import org.letstrust.model.DidMethod
import org.letstrust.services.did.DidService
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
    var uw = UserWallet()

    fun requestAccess(): Boolean {
        println("2. Sign-On")
        var authReq = rp.signOn()

        println("6. Process Authentication Request: 302 openid://")
        println("7. Process Authentication Request: openid://")
        val authResp = uw.processAuthenticationRequest(authReq)
        println("11. Authentication Response: Callback /callback 302")
        println("17. Success")
        return rp.callback(authResp)
    }
}

class UserWallet() {
    val didUrlUser by lazy {
        DidService.create(DidMethod.web)
    }

    fun processAuthenticationRequest(authReq: String): String {
        println("8. OIDC Validation")
        println("9. DID AuthN validation")
        println("10. Generate Authentication Response")
        val authResp = ""
        return authResp
    }
}

class RelyingParty() {
    val ew = EnterpriseWallet()
    fun signOn(): String {
        val authReq = ew.auth()

        println("5. Return Authentication Request")

        return authReq
    }

    fun callback(authResp: String): Boolean {
        println("12. Authentication Response: Callback /callback")
        return ew.token(authResp)
        print("15. Success")
    }
}

class EnterpriseWallet() {

    val didUrlRp by lazy {
        DidService.create(DidMethod.web)
    }


    fun auth(): String {
        println("3. Auth")

        println("4. Generate Authentication Request")
        val authRequest = "openid://?response_type=id_token\n" +
                "    &client_id=https%3A%2F%2Frp.example.com%2Fcb\n" +
                "    &scope=openid%20did_authn\n" +
                "    &request=<authentication-request-JWS>"
        return authRequest
    }

    fun token(authResp: String): Boolean {
        println("13. /token <Authentication Response>")

        println("14. OIDC Validation")

        println("15. DID AuthN validation")
        return true
    }
}


