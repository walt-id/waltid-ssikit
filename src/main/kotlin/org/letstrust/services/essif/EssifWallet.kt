package org.letstrust.services.essif

object EssifWalletClient {
    fun generateAuthenticationRequest(): String {
        return EssifWallet.generateAuthenticationRequest()
    }

    fun openSession(authResp: String): String {
        return EssifWallet.openSession(authResp)
    }
}

object EssifWallet {
    ///////////////////////////////////////////////////////////////////////////
    // Client
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Runs DID + VC Auth (optional)
     * Used for presenting as well as receiving VCs
     */
    fun authenticate() {
        val authReq = EssifWalletClient.generateAuthenticationRequest()
        validateAuthenticationRequest(authReq)
        val authResp = this.generateAuthenticationResponse(authReq)
        val response = EssifWalletClient.openSession(authResp)
        println(authReq)
    }

    private fun validateAuthenticationRequest(authReq: String): String {
        return authReq
    }

    private fun generateAuthenticationResponse(authReq: String): String {
        return authReq
    }


    ///////////////////////////////////////////////////////////////////////////
    // Server
    ///////////////////////////////////////////////////////////////////////////

    fun generateAuthenticationRequest(): String {
        return "TODO"
    }

    fun openSession(authResp: String): String {
        this.validateAuthenticationResponse(authResp)
        return this.generateEncryptedAccessToken(authResp)
    }

    private fun validateAuthenticationResponse(authResp: String): String {
        return authResp
    }

    private fun generateEncryptedAccessToken(authResp: String): String {
        return authResp
    }
}

fun main() = EssifWallet.authenticate()
