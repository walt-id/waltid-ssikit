package org.letstrust.essif

import org.junit.Test
import org.letstrust.s.essif.EosService

import org.letstrust.services.essif.WalletService

// https://ec.europa.eu/cefdigital/wiki/display/BLOCKCHAININT/2.+Main+Flow%3A+VC-Request+-+Onboarding+Flow
class EbsiOnboardingFlowTest {

    @Test
    fun testEbsiVCRequestOnboardingFlow() {

        println("1 Request V.ID (Manually)")
        val credentialRequest = EosService.requestCredentialUri()
        println("3 Trigger Wallet")
        val reqDidProve = WalletService.requestVerifiableId()
        println("6. Notify DID ownership request")
        println("7. Create a new DID")
        WalletService.didGeneration()

        WalletService.getVerifiableId()
        println("14. Successful process")

    }
}
