package org.letstrust.essif

import org.letstrust.model.AuthRequestResponse
import org.letstrust.services.essif.IEosService

class EosServiceMock: IEosService {
    override fun authenticationRequests(): AuthRequestResponse {
        TODO("Not yet implemented")
    }

    override fun authenticationResponse(idToken: String, bearerToken: String): String {
        TODO("Not yet implemented")
    }

    override fun siopSession(idToken: String, bearerToken: String): String {
        TODO("Not yet implemented")
    }

    override fun onboards(): String {
        TODO("Not yet implemented")
    }

    override fun signedChallenge(signedChallenge: String): String {
        TODO("Not yet implemented")
    }

    override fun requestVerifiableCredential(): String {
        TODO("Not yet implemented")
    }

    override fun requestCredentialUri(): String {
        TODO("Not yet implemented")
    }

    override fun didOwnershipResponse(didOwnershipResp: String): String {
        TODO("Not yet implemented")
    }

    override fun getCredential(id: String): String {
        TODO("Not yet implemented")
    }

    override fun getCredentials(isUserAuthenticated: Boolean): String {
        TODO("Not yet implemented")
    }
}
