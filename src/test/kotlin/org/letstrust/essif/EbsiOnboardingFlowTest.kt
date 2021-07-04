package org.letstrust.essif

import mu.KotlinLogging
import org.junit.Test
import org.letstrust.services.essif.EssifFlowRunner


class EbsiOnboardingFlowTest {
    private val log = KotlinLogging.logger {}

    @Test
    fun testEbsiVCRequestOnboardingFlow() {

        EssifFlowRunner.onboard()

    }
}
