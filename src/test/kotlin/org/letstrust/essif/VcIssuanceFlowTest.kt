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

    }
}
