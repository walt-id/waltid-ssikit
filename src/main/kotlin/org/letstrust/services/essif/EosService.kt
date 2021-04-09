package org.letstrust.s.essif

import org.letstrust.services.essif.EnterpriseWalletService
import org.letstrust.services.essif.mock.DidRegistry
import java.io.File

object EosService {

    private fun readEssif(fileName: String) = File("src/test/resources/essif/${fileName}.json").readText(Charsets.UTF_8)

    // POST /onboards
    // returns DID ownership
    fun onboards(): String {
        println("6. [Eos] Request DID ownership")
        return "DID ownership req"
    }

    fun signedChallenge(signedChallenge: String): String {
        println("8. [Eos] Validate DID Document")
        println("9. [Eos] GET /identifiers/{did}")
        DidRegistry.get("did")
        println("10. [Eos] 404 Not found")
        println("11. [Eos] Generate Verifiable Authorization")
        return "Verifiable Authorization"
    }

    fun requestVerifiableId(): String {
        println("4. [Eos] Request V.ID")
        return "didOwnershipReq"
    }

    fun requestCredentialUri(): String {
        println("2 [Eos] Request Credential (QR, URI, ...)")
        return "uri"
    }

    fun didOwnershipResponse(): String {
        println("8. [Eos] Response DID ownership")
        println("9. [Eos] Validate DID ownership")
        return "200 V.ID Request OK"
    }

    fun getCredentials(isUserAuthenticated: Boolean = false): String {
        if (isUserAuthenticated) {
            println("12. [Eos] [GET]/credentials")
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
