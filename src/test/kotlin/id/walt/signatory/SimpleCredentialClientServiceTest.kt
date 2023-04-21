package id.walt.signatory

import id.walt.common.createBaseToken
import id.walt.common.deriveRevocationToken
import id.walt.servicematrix.ServiceMatrix
import id.walt.signatory.rest.SignatoryRestAPI
import id.walt.signatory.revocation.TokenRevocationCheckParameter
import id.walt.signatory.revocation.TokenRevocationConfig
import id.walt.signatory.revocation.simplestatus2022.SimpleCredentialClientService
import id.walt.signatory.revocation.simplestatus2022.SimpleCredentialStatus2022StorageService
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.style.AnnotationSpec

class SimpleCredentialClientServiceTest : AnnotationSpec() {

    init {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
        SimpleCredentialStatus2022StorageService.clearRevocations()
    }

    private val SIGNATORY_API_HOST = "localhost"
    private val SIGNATORY_API_PORT = 7001
    private val SIGNATORY_API_URL = "http://$SIGNATORY_API_HOST:$SIGNATORY_API_PORT"

    @BeforeClass
    fun startServer() {
        SignatoryRestAPI.start(SIGNATORY_API_PORT)
    }

    @AfterClass
    fun teardown() {
        SignatoryRestAPI.stop()
    }

    @Test
    fun test() {
        val revocationsBase = "$SIGNATORY_API_URL/v1/credentials/token"

        val rs = SimpleCredentialClientService()

        val baseToken = createBaseToken()
        println(baseToken)

        val revocationToken = deriveRevocationToken(baseToken)
        println(revocationToken)

        var result = rs.checkRevocation(TokenRevocationCheckParameter("$revocationsBase/$revocationToken"))
        println(result)

        rs.revoke(TokenRevocationConfig("$revocationsBase/$baseToken"))

        result = rs.checkRevocation(TokenRevocationCheckParameter("$revocationsBase/$revocationToken"))
        println(result)

    }
}
