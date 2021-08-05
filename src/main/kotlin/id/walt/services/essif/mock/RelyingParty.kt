package id.walt.services.essif.mock

import id.walt.services.essif.enterprisewallet.EnterpriseWalletService

object RelyingParty {

    private val enterpriseWalletService = EnterpriseWalletService.getService()

    fun signOn(): String {
        val authReq = enterpriseWalletService.generateOidcAuthRequest()

        println("5/4. [RP] 200 Return Authentication Request")
        println("5. [RP] Generate QR, URI")

        enterpriseWalletService.getSession("sessionId")

        return authReq
    }

    fun callback(authResp: String): Boolean {
        println("12. [RP] Authentication Response: Callback /callback")
        return enterpriseWalletService.token(authResp)
        print("15. [RP] Success")
    }

    fun getSession(sessionId: String) {
        enterpriseWalletService.getSession(sessionId)
    }
}
