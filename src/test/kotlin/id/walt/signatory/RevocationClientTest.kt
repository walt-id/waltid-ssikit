package id.walt.signatory

import id.walt.servicematrix.ServiceMatrix
import id.walt.signatory.rest.SignatoryRestAPI
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.style.AnnotationSpec

class RevocationClientTest : AnnotationSpec() {

    init {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
        RevocationService.clearRevocations()
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
        val revocationsBase = "$SIGNATORY_API_URL/v1/revocations"

        val rs = RevocationClientService.getService()

        val baseToken = rs.createBaseToken()
        println(baseToken)

        val revocationToken = rs.deriveRevocationToken(baseToken)
        println(revocationToken)

        var result = rs.checkRevoked("$revocationsBase/$revocationToken")
        println(result)

        rs.revoke("$revocationsBase/$baseToken")

        result = rs.checkRevoked("$revocationsBase/$revocationToken")
        println(result)

    }
}
