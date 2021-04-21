package org.letstrust.essif

import org.junit.Test
import org.letstrust.services.essif.EssifFlowRunner
import org.letstrust.services.essif.UserWalletService
import org.letstrust.services.essif.mock.RelyingParty

class VcExchangeFlowTest {

    @Test
    fun testVcExchangeFlow() {
        EssifFlowRunner.vcExchange()
    }
}
