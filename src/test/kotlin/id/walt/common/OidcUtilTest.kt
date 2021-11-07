package id.walt.common

import id.walt.servicematrix.ServiceMatrix
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.style.AnnotationSpec

class OidcUtilTest : AnnotationSpec() {

    @Before
    fun setup() {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    }

    @Test
    fun authenticationRequestTest() {
//        val didServer = DidService.create(DidMethod.ebsi)
//        val redirectUri = "http://localhost:8080/redirect"
//        val callback = "http://localhost:8080/callback"
//        val nonce = UUID.randomUUID().toString()
//
//        val oidcReq = OidcUtil.generateOidcAuthenticationRequest(didServer, redirectUri, callback, nonce)
//
//        val didAuthReq = OidcUtil.validateOidcAuthenticationRequest(oidcReq)
//
//        callback shouldBe didAuthReq.callback
//        nonce shouldBe didAuthReq.nonce
//        redirectUri shouldBe didAuthReq.client_id
//        redirectUri shouldBe didAuthReq.authenticationRequestJwt!!.authRequestPayload.client_id
//        didServer shouldBe didAuthReq.authenticationRequestJwt!!.authRequestPayload.iss
    }
}
