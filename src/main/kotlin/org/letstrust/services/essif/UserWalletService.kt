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

        this.generateDidAuthResponse(didAuthRequest)
    }

    fun validateVcExchangeRequest(vcExchangeRequest: String) {
        println("10. [UWallet] Validate request")

        this.generateDidAuthResponse(vcExchangeRequest)

    }

    fun generateDidAuthResponse(didAuthRequest: String) {
        println("13/11. [UWallet] Generate (DID-)Auth Response")
        println("14/12. [UWallet] /callback (DID-)Auth Response")
        val vcToken = EnterpriseWalletService.validateDidAuthResponse("didAuthResp")
        println("16/14. [UWallet] 200 OK")

    }
}
