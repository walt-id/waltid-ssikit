package id.walt.signatory

import id.walt.common.createBaseToken
import id.walt.common.deriveRevocationToken
import id.walt.servicematrix.ServiceMatrix
import id.walt.signatory.revocation.SimpleCredentialStatus2022Service
<<<<<<< HEAD
import id.walt.signatory.revocation.TokenRevocationResult
=======
>>>>>>> eddd42a2 (refactor: revocation-service)
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class RevocationServiceTest : AnnotationSpec() {

    init {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
        SimpleCredentialStatus2022Service.clearRevocations()
    }

    //    @Test TODO: fix
    fun test() {
        val service = RevocationClientService.getService()

        val baseToken = createBaseToken()
        println("New base token: $baseToken")

        val revocationToken = deriveRevocationToken(baseToken)
        println("Revocation token derived from base token: $revocationToken")

        println("Check revoked with derived token: $revocationToken")
<<<<<<< HEAD
        val result1 = SimpleCredentialStatus2022Service.checkRevoked(revocationToken) as TokenRevocationResult
=======
        val result1 = SimpleCredentialStatus2022Service.checkRevoked(revocationToken)
>>>>>>> eddd42a2 (refactor: revocation-service)
        result1.isRevoked shouldBe false
        result1.timeOfRevocation shouldBe null

        println("Revoke with base token: $baseToken")
        SimpleCredentialStatus2022Service.revokeToken(baseToken)

        println("Check revoked with derived token: $revocationToken")
<<<<<<< HEAD
        val result2 = SimpleCredentialStatus2022Service.checkRevoked(revocationToken) as TokenRevocationResult
=======
        val result2 = SimpleCredentialStatus2022Service.checkRevoked(revocationToken)
>>>>>>> eddd42a2 (refactor: revocation-service)
        result2.isRevoked shouldBe true
        result2.timeOfRevocation shouldNotBe null
    }
}
