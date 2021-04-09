package org.letstrust.services.essif

import org.letstrust.model.DidMethod
import org.letstrust.s.essif.EosService
import org.letstrust.services.did.DidService

object EnterpriseWalletService {

    // https://besu.hyperledger.org/en/stable/HowTo/Send-Transactions/Account-Management/
    fun didGeneration() {
        println("1. [EWallet] Generate ETH address (keys)")
        println("2. [EWallet] Generate DID Controlling Keys)")
        println("3. [EWallet] Store DID Controlling Private Key")
        println("4. [EWallet] Generate DID Document")
    }

    fun authorizationRequest() {
        println("5. [EWallet] POST /onboards")
        val didOwnershipReq = EosService.onboards()
        println("7. [EWallet] Signed Challenge")
        val verifiableAuthorization = EosService.signedChallenge("signedChallenge")
        println("12. [EWallet] 201 V. Authorization")
    }

    fun generateDidAuthReq() {
        println("3. [EWallet] Generate <DID-Auth Request>")
    }

    fun requestVerifiableId() {
        val didOwnershipReq = EosService.requestVerifiableId()
        println("5. [EWallet] Request DID prove")
    }

    fun getVerifiableId(): String {
        val vIdRequest = EosService.didOwnershipResponse()
        EosService.getCredentials()
        println("13 [EWallet] 200 <V.ID>")
        return vIdRequest
    }

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

    fun validateDidAuthResponse(didAuthResp: String) : String {
        println("15. [EWallet]  Validate response")
        return "vcToken"
    }

    fun getSession(sessionId: String): String {
        println("8. [EWallet]428 (no content)")
        return "notfound - or session"
    }
}
