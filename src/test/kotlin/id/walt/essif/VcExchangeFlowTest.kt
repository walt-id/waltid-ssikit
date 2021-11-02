package id.walt.essif

import id.walt.services.essif.EssifClient
import io.kotest.core.spec.style.AnnotationSpec

class VcExchangeFlowTest : AnnotationSpec() {

    @Test
    fun testVcExchangeFlow() {
        EssifClient.vcExchange()
    }
}
