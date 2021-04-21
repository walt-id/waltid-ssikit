package org.letstrust.essif

import org.junit.Test
import org.letstrust.services.essif.EssifFlowRunner
import org.letstrust.services.essif.UserWalletService

class AuthorizationApiTest {

    @Test
    fun testAuthApiFlow() {
       EssifFlowRunner.authApi()
    }
}
