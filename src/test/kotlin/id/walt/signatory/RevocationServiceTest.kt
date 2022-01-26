package id.walt.signatory

import id.walt.servicematrix.ServiceMatrix
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class RevocationServiceTest : AnnotationSpec() {

    init {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
        RevocationService.clearRevocations()
    }

    @Test
    fun test() {
        val service = RevocationClientService.getService()

        val baseToken = service.createBaseToken()
        println("New base token: $baseToken")

        val revocationToken = RevocationService.getRevocationToken(baseToken)
        println("Revocation token derived from base token: $revocationToken")

        println("Check revoked with derived token: $revocationToken")
        val result1 = RevocationService.checkRevoked(revocationToken)
        result1.isRevoked shouldBe false
        result1.timeOfRevocation shouldBe null

        println("Revoke with base token: $baseToken")
        RevocationService.revokeToken(baseToken)

        println("Check revoked with derived token: $revocationToken")
        val result2 = RevocationService.checkRevoked(revocationToken)
        result2.isRevoked shouldBe true
        result2.timeOfRevocation shouldNotBe null
    }
}
