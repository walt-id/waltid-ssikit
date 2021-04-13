package org.letstrust.essif

import mu.KotlinLogging
import org.letstrust.s.essif.EosService
import org.letstrust.services.essif.EnterpriseWalletService



// https://ec.europa.eu/cefdigital/wiki/display/BLOCKCHAININT/2.+Main+Flow%3A+VC-Request+-+Onboarding+Flow
class EbsiOnboardingFlowTest {
    private val log = KotlinLogging.logger {}
    //TODO @Test
    fun testEbsiVCRequestOnboardingFlow() {

        println("1 Request V.ID (Manually)")
        val credentialRequestUri = EosService.requestCredentialUri()
        log.debug { "credentialRequest: $credentialRequestUri" }
        println("3 Trigger Wallet")
        val didOwnershipReq = EnterpriseWalletService.requestVerifiableId(credentialRequestUri)
        log.debug { "didOwnershipReq: $didOwnershipReq" }
        println("6. Notify DID ownership request")
        println("7. Create a new DID")

        val didOfLegalEntity = EnterpriseWalletService.createDid()

        val verifiableId = EnterpriseWalletService.getVerifiableId(didOwnershipReq, didOfLegalEntity)
        log.debug { "verifiableId: $verifiableId" }
        println("14. Successful process")

    }
}
