package org.letstrust.essif

import org.junit.Test
import org.letstrust.services.essif.UserWalletService

class VcExchangeFlowTest {

    val rp = RelyingParty()

    @Test
    fun testVcExchangeFlow() {
        println("1 Request Login")
        val vcExchangeRequest = rp.signOn()
        println("6. QR, URI")
        UserWalletService.validateVcExchangeRequest(vcExchangeRequest)
        println("15. Credentials share successfully")

        rp.getSession("sessionId")
        println("18. Process completed successsfully")
    }
}
