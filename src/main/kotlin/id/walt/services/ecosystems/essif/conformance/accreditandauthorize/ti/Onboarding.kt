package id.walt.services.ecosystems.essif.conformance.accreditandauthorize.ti

import id.walt.services.ecosystems.essif.conformance.ConformanceLog
import id.walt.services.ecosystems.essif.conformance.CredentialIssuanceFlow
import id.walt.services.ecosystems.essif.conformance.Test

object Onboarding : Test {
    override suspend fun run() {
        ConformanceLog.log("Onboarding")
        requestCredential()
        registerDidDocument("")
    }

    suspend fun requestCredential() {
        ConformanceLog.log("Request VerifiableAuthorisationToOnboard")
        val credential = CredentialIssuanceFlow.getCredential("VerifiableAuthorizationToOnboard")
    }

    fun registerDidDocument(did: String) {
        ConformanceLog.log("Register your DID document into the DID Registry")
    }
}
