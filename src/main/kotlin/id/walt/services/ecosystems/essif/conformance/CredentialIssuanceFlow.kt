package id.walt.services.ecosystems.essif.conformance

object CredentialIssuanceFlow {
    fun getCredential(type: String) {
        val queryParams = authorizeRequest()
        val idTokenParams = directPostIdTokenRequest()
        val authToken = authTokenRequest()
        val jwtCredential = credentialRequest()
        decodeCredential(jwtCredential)
    }

    private fun authorizeRequest() {}
    private fun directPostIdTokenRequest() {}
    private fun authTokenRequest() {}
    private fun credentialRequest(): String {
        TODO()
    }

    private fun decodeCredential(jwt: String) {}

    private fun getCredentialRequestedTypesList(type: String) = listOf(
        "VerifiableCredential", "VerifiableAttestation"
    ).apply {
        when (type) {
            "VerifiableAccreditationToAttest", "VerifiableAccreditationToAccredit" -> this.plus("VerifiableAccreditation")
            else -> {}
        }
    }.plus(type)
}
