package org.letstrust.essif

import org.junit.Test
import org.letstrust.s.essif.EosService
import org.letstrust.services.essif.UserWalletService

// https://ec.europa.eu/cefdigital/wiki/display/BLOCKCHAININT/2.+Main+Flow%3A+VC-Request+-+Onboarding+Flow
class VcIssuanceFlowTest {

    @Test
    fun testVcIssuanceFlow() {

        println("1 Request VC (Manually)")
        val didAuthRequest = EosService.getCredentials()
        println("6. QR, URI, ...")
        println("9. Trigger Wallet (Scan QR, enter URI, ...)")
        UserWalletService.validateDidAuthRequest(didAuthRequest)
        println("11. DID-Auth request")
        println("12. Consent")
        val didAuthResp = UserWalletService.generateDidAuthResponse(didAuthRequest)

        println("17 VC requested successfully")
        println("20 Process completed successfully")
        val credential = EosService.getCredentials(true) // user is authenticated (VC token is received); TODO: Align with spec, as the request goes to the EWallet there
        println("21 Credential received")
    }
}
