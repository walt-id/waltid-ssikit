package org.letstrust.essif

import org.junit.Test
import org.letstrust.services.essif.EssifFlowRunner

class AuthorizationApiTest {

    @Test
    fun testAuthApiFlow() {
        EssifFlowRunner.authApi()
    }
}
