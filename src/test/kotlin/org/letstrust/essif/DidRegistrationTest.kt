package org.letstrust.essif

import org.junit.Test
import org.letstrust.services.essif.WalletService

class DidRegistrationTest {
    @Test
    fun testEbsiDidRegistration() {
        WalletService.didGeneration()

        WalletService.authorizationRequest()
    }
}
