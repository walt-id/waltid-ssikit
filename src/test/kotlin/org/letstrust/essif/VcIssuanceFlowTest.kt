package org.letstrust.essif

import org.junit.Test
import org.letstrust.common.readEssif
import org.letstrust.services.essif.EnterpriseWalletService
import org.letstrust.services.essif.EosService
import org.letstrust.services.essif.EssifFlowRunner
import org.letstrust.services.essif.UserWalletService

class VcIssuanceFlowTest {

    @Test
    fun generateDidAuthRequest() {
        println(EnterpriseWalletService.generateDidAuthRequest())
    }

    @Test
    fun validateDidAuthRequest() {
        val didAuthReq = readEssif("onboarding-did-ownership-req")

        // TODO validate data structure

        println(didAuthReq)
    }

    @Test
    fun testVcIssuanceFlow() {
        EssifFlowRunner.vcIssuance()
    }
}
