package org.letstrust.essif

import org.junit.Test
import org.letstrust.services.essif.EnterpriseWalletService

class DidRegistrationTest {
    @Test
    fun testEbsiDidRegistration() {
        EnterpriseWalletService.didGeneration()

        EnterpriseWalletService.requestVerifiableAuthorization()
    }
}
