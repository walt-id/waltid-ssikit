package org.letstrust.essif

import org.junit.Test

class OdicSsiFlowTest {
    @Test
    fun runOidcSsiFlowTest() {
        println("1. Request access")
        UserAgent().requestAccess()
        println("17. Process done successfully")
    }
}

class UserAgent() {
    var rp = RelyingParty()
    var uw = UserWallet()

    fun requestAccess() {
        println("2. Sign-On")
        rp.signOn()

        println("6. Process Authentication Request: 302 openid://")
        println("7. Process Authentication Request: openid://")
        uw.processAuthenticationRequest()
        println("11. Authentication Response: Callback /callback 302")
        rp.callback()
        println("17. Success")
    }

}

class UserWallet() {
    fun processAuthenticationRequest() {
        println("8. OIDC Validation")
        println("9. DID AuthN validation")
        println("10. Generate Authentication Response")
    }
}

class RelyingParty() {
    val ew = EnterpriseWallet()
    fun signOn() {
        ew.auth()

        println("5. Return Authentication Request")

    }

    fun callback() {
        println("12. Authentication Response: Callback /callback")
        ew.token()
        print("15. Success")
    }
}

class EnterpriseWallet() {
    fun auth() {
        println("3. Auth")

        println("4. Generate Authentication Request")

    }

    fun token() {
        println("13. /token <Authentication Response>")

        println("14. OIDC Validation")

        println("15. DID AuthN validation")
    }
}

