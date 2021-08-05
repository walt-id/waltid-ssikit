package id.walt.essif

import io.kotest.core.spec.style.AnnotationSpec
import id.walt.services.essif.EssifFlowRunner

class VcExchangeFlowTest : AnnotationSpec() {

    @Test
    fun testVcExchangeFlow() {
        EssifFlowRunner.vcExchange()
    }
}
