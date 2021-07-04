package org.letstrust.essif

import org.junit.Test
import org.letstrust.services.essif.EssifFlowRunner

class VcIssuanceFlowTest {

    @Test
    fun testVcIssuanceFlow() {
        EssifFlowRunner.vcIssuance()
    }
}
