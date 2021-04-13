package org.letstrust.services.essif

import mu.KotlinLogging
import org.letstrust.common.readEssif
import org.letstrust.s.essif.EosService
import org.letstrust.services.essif.mock.DidRegistry

object EnterpriseWalletService {

    private val log = KotlinLogging.logger {}

//    val didUrlRp by lazy {
//        DidService.create(DidMethod.web)
//    }

    fun createDid(): String {
        val did = didGeneration()
        log.debug { "did: $did" }

        val verifiableAuthorization = requestVerifiableAuthorization(did)
        log.debug { "verifiableAuthorization: $verifiableAuthorization" }

        val unsignedTransaction = DidRegistry.insertDidDocument()
        println("16. [EWallet] 200 <unsigned transaction>")
        println("17. [EWallet] Generate <signed transaction>")
        val signedTransaction = ""
        DidRegistry.signedTransaction(signedTransaction)
        return did
    }

    // https://besu.hyperledger.org/en/stable/HowTo/Send-Transactions/Account-Management/
    fun didGeneration(): String {
        println("1. [EWallet] Generate ETH address (keys)")
        println("2. [EWallet] Generate DID Controlling Keys)")
        println("3. [EWallet] Store DID Controlling Private Key")
        println("4. [EWallet] Generate DID Document")
        return "did"
    }

    fun requestVerifiableAuthorization(did: String): String {
        println("5. [EWallet] POST /onboards")
        val didOwnershipReq = EosService.onboards()
        log.debug { "didOwnershipReq: $didOwnershipReq" }
        log.debug { "didOwnershipReqHeader:" + readEssif("onboarding-onboards-resp-header") }
        log.debug { "didOwnershipReqBody: " + readEssif("onboarding-onboards-resp-body") }
        println("7. [EWallet] Signed Challenge")

        val signedChallenge = readEssif("onboarding-onboards-callback-req")
        log.debug { "signedChallenge: $signedChallenge" }
        val verifiableAuthorization = EosService.signedChallenge(signedChallenge)
        println("12. [EWallet] 201 V. Authorization")
        return verifiableAuthorization
    }

    fun requestVerifiableId(credentialRequestUri: String): String {
        val didOwnershipReq = EosService.requestVerifiableId(credentialRequestUri)
        log.debug { didOwnershipReq }
        println("5. [EWallet] Request DID prove")
        return didOwnershipReq
    }

    fun getVerifiableId(didOwnershipReq: String, didOfLegalEntity: String): String {
        // TODO Build didOwnershipResp
        val didOwnershipResp = readEssif("onboarding-did-ownership-resp")
        val vIdRequest = EosService.didOwnershipResponse(didOwnershipResp)
        log.debug { "vIdRequest: $vIdRequest" }
        val vId = EosService.getCredential("id")
        println("13 [EWallet] 200 <V.ID>")
        return vId
    }


    fun generateOidcAuthRequest(): String {
        println("3/2. [EWallet] Auth /auth")

        println("4/3. [EWallet] Generate Authentication Request")
        val authRequest = "openid://?response_type=id_token\n" +
                "    &client_id=https%3A%2F%2Frp.example.com%2Fcb\n" +
                "    &scope=openid%20did_authn\n" +
                "    &request=<authentication-request-JWS>"
        return authRequest
    }

    fun generateDidAuthRequest() {
        println("3. [EWallet] Generate <DID-Auth Request>")
    }


    fun token(authResp: String): Boolean {
        println("13. [EWallet] /token <Authentication Response>")

        println("14. [EWallet] OIDC Validation")

        println("15. [EWallet] DID AuthN validation")
        return true
    }

    fun validateDidAuthResponse(didAuthResp: String): String {
        println("15/13. [EWallet]  Validate response")
        return "vcToken"
    }

    fun getSession(sessionId: String): String {
        println("7/16. [EWallet] /sessions/{id}")
        println("8/17. [EWallet] 428 (no content)")
        return "notfound - or session"
    }

    // used in Trusted Issuer Onboarding
    fun onboardTrustedIssuer(scanQrUri: String) {
        // Send information to the Trusted Accreditation Organization
        println("9. [EWallet] GET /sessions/{id}")
        println("10. [EWallet] 200 <Sessions>")
        println("11. [EWallet] [POST] /sessions")
    }


}
