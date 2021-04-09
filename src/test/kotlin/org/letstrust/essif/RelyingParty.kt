package org.letstrust.essif

import org.letstrust.services.essif.EnterpriseWalletService

class RelyingParty() {
    fun signOn(): String {
        val authReq = EnterpriseWalletService.auth()

        println("5/4. 200 Return Authentication Request")
        println("5. Generate QR, URI")

        EnterpriseWalletService.getSession("sessionId")

        return authReq
    }

    fun callback(authResp: String): Boolean {
        println("12. Authentication Response: Callback /callback")
        return EnterpriseWalletService.token(authResp)
        print("15. Success")
    }

    fun getSession(sessionId: String) {
        EnterpriseWalletService.getSession(sessionId)
    }
}
