package org.letstrust.essif

import org.letstrust.services.essif.EnterpriseWalletService

class RelyingParty() {
    fun signOn(): String {
        val authReq = EnterpriseWalletService.generateOidcAuthRequest()

        println("5/4. [RP] 200 Return Authentication Request")
        println("5. [RP] Generate QR, URI")

        EnterpriseWalletService.getSession("sessionId")

        return authReq
    }

    fun callback(authResp: String): Boolean {
        println("12. [RP] Authentication Response: Callback /callback")
        return EnterpriseWalletService.token(authResp)
        print("15. [RP] Success")
    }

    fun getSession(sessionId: String) {
        EnterpriseWalletService.getSession(sessionId)
    }
}
