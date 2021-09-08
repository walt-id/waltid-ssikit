package id.walt.essif

import io.kotest.core.spec.style.AnnotationSpec
import id.walt.services.essif.EssifClient

class VcExchangeFlowTest : AnnotationSpec() {

    @Test
    fun testVcExchangeFlow() {
        EssifClient.vcExchange()
    }
}
