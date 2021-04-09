package org.letstrust.services.essif

object UserWalletService {

//    val didUrlUser by lazy {
//        DidService.create(DidMethod.web)
//    }

    fun processAuthenticationRequest(authReq: String): String {
        println("8. [UWallet] OIDC Validation")
        println("9. [UWallet] DID AuthN validation")
        println("10. [UWallet] Generate Authentication Response")
        val authResp = ""
        return authResp
    }

    fun validateDidAuthRequest(didAuthRequest: String) {
        println("10. [UWallet] Validate request")

    }

    fun generateDidAuthResponse(didAuthRequest: String) {
        println("13. [UWallet] Generate DID-Auth Response")
        println("14. [UWallet] /callback DID-Auth Response")
        val vcToken = EnterpriseWalletService.validateDidAuthResponse("didAuthResp")
        println("16. [UWallet] 200 OK")

    }
}
