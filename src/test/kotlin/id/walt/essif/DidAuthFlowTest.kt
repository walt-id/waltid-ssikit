package id.walt.essif

import id.walt.services.essif.mock.UserAgent
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe

// https://ec.europa.eu/cefdigital/wiki/display/BLOCKCHAININT/2.+DID-Auth+Flow
class DidAuthFlowTest : AnnotationSpec() {

    @Test
    fun testDidAuthFlow() {
        println("1. Request access")
        val ret = UserAgent().requestAccess()
        ret shouldBe true
        println("17. Process done successfully")
    }
}
