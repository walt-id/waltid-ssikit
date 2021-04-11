package org.letstrust.s.essif

import mu.KotlinLogging
import org.letstrust.common.readEssif
import org.letstrust.services.essif.EnterpriseWalletService
import org.letstrust.services.essif.mock.DidRegistry

private val log = KotlinLogging.logger {}

object EosService {

    // POST /onboards
    // returns DID ownership
    fun onboards(): String {
        println("6. [Eos] Request DID ownership")
        return readEssif("onboarding-onboards-resp")
    }

    fun signedChallenge(signedChallenge: String): String {

        val header = readEssif("onboarding-onboards-callback-req-header")
        val body = readEssif("onboarding-onboards-callback-req-body")

        log.debug { "header: $header" }
        log.debug { "body: $body" }

        println("8. [Eos] Validate DID Document")
        println("9. [Eos] GET /identifiers/{did}")
        DidRegistry.get("did")
        println("10. [Eos] 404 Not found")
        println("11. [Eos] Generate Verifiable Authorization")
        val verifiableAuthorization = readEssif("onboarding-onboards-callback-resp")
        return verifiableAuthorization
    }

    fun requestVerifiableId(): String {
        println("4. [Eos] Request V.ID")
        return readEssif("onboarding-did-ownership-req")
    }

    fun requestCredentialUri(): String {
        println("2 [Eos] Request Credential (QR, URI, ...)")
        return "uri"
    }

    fun didOwnershipResponse(didOwnershipResp: String): String {
        println("8. [Eos] Response DID ownership")
        log.debug { "didOwnershipResp: $didOwnershipResp" }
        println("9. [Eos] Validate DID ownership")
        val didOwnershipRespHeader = readEssif("onboarding-did-ownership-resp-header")
        log.debug { "didOwnershipRespHeader: $didOwnershipRespHeader" }
        val didOwnershipRespBody = readEssif("onboarding-did-ownership-resp-body")
        log.debug { "didOwnershipRespBody: $didOwnershipRespBody" }
        val vIdRequestOkResp = readEssif("onboarding-vid-req-ok")
        return vIdRequestOkResp
    }

    fun getCredential(id: String): String {
        println("12. [Eos] [GET]/credentials")
        return readEssif("onboarding-vid")
    }

    fun getCredentials(isUserAuthenticated: Boolean = false): String {
        if (isUserAuthenticated) {
            return readEssif("vc-issuance-auth-req")
        } else {
            println("2. [Eos] [GET]/credentials")
            EnterpriseWalletService.generateDidAuthRequest()
            println("4. [Eos] 200 <DID-Auth Req>")
            println("5. [Eos] Generate QR, URI")
            // TODO: Trigger job for [GET] /sessions/{id}
            val str = EnterpriseWalletService.getSession("sessionID")
            return readEssif("vc-issuance-auth-req")
        }

    }
}
