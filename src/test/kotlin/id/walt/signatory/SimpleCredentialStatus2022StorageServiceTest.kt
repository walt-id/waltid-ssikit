package id.walt.signatory

import id.walt.common.createBaseToken
import id.walt.common.deriveRevocationToken
import id.walt.servicematrix.ServiceMatrix
import id.walt.signatory.revocation.TokenRevocationStatus
import id.walt.signatory.revocation.simplestatus2022.SimpleCredentialClientService
import id.walt.signatory.revocation.simplestatus2022.SimpleCredentialStatus2022StorageService
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class SimpleCredentialStatus2022StorageServiceTest : AnnotationSpec() {

    init {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
        SimpleCredentialStatus2022StorageService.clearRevocations()
    }

    @Test
    fun test() {
        val service = SimpleCredentialClientService()

        val baseToken = createBaseToken()
        println("New base token: $baseToken")

        val revocationToken = deriveRevocationToken(baseToken)
        println("Revocation token derived from base token: $revocationToken")

        println("Check revoked with derived token: $revocationToken")
        val result1 = SimpleCredentialStatus2022StorageService.checkRevoked(baseToken) as TokenRevocationStatus
        result1.isRevoked shouldBe false
        result1.timeOfRevocation shouldBe null

        println("Revoke with base token: $baseToken")
        SimpleCredentialStatus2022StorageService.revokeToken(baseToken)

        println("Check revoked with derived token: $revocationToken")

        val result2 = SimpleCredentialStatus2022StorageService.checkRevoked(baseToken) as TokenRevocationStatus
        result2.isRevoked shouldBe true
        result2.timeOfRevocation shouldNotBe null
    }
}
