package org.letstrust.essif

import mu.KotlinLogging
import org.junit.Test
import org.letstrust.services.essif.EnterpriseWalletService

private val log = KotlinLogging.logger {}

// Subflow from EBSI Onboarding Flow
// https://ec.europa.eu/cefdigital/wiki/display/BLOCKCHAININT/4.+SUB-Flow%3A+DID%28-key%29+Registration+-++DID+Registration+flow
class DidRegistrationTest {
    @Test
    fun testEbsiDidRegistration() {

        EnterpriseWalletService.didGeneration()

        val verifiableAuthoriztation = EnterpriseWalletService.requestVerifiableAuthorization()
        log.debug { "verifiableAuthoriztation: $verifiableAuthoriztation" }
    }
}
