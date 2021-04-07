package org.letstrust.s.essif

import org.letstrust.services.essif.DidRegistry

object EosService {

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

    fun didOwnershipResponse() : String {
        println("8. [Eos] Response DID ownership")
        println("9. [Eos] Validate DID ownership")
        return "200 V.ID Request OK"
    }

    fun getCredentials() {
        println("12. [Eos] [GET]/credentials")
    }
}
