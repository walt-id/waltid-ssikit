package org.letstrust.common

import id.walt.servicematrix.ServiceMatrix
import org.junit.Before
import org.junit.Test
import org.letstrust.model.DidMethod
import org.letstrust.services.did.DidService
import java.util.*
import kotlin.test.assertEquals

class OidcUtilTest {

    @Before
    fun setup(){
        ServiceMatrix("service-matrix.properties")
    }

    @Test
    fun authenticationRequestTest() {
        val didServer = DidService.create(DidMethod.ebsi)
        val redirectUri = "http://localhost:8080/redirect"
        val callback = "http://localhost:8080/callback"
        val nonce = UUID.randomUUID().toString()

        val oidcReq = OidcUtil.generateOidcAuthenticationRequest(didServer, redirectUri, callback, nonce)

        val didAuthReq = OidcUtil.validateOidcAuthenticationRequest(oidcReq)

        assertEquals(callback, didAuthReq.callback)
        assertEquals(nonce, didAuthReq.nonce)
        assertEquals(redirectUri, didAuthReq.client_id)
        assertEquals(redirectUri, didAuthReq.authenticationRequestJwt.authRequestPayload.client_id)
        assertEquals(didServer, didAuthReq.authenticationRequestJwt.authRequestPayload.iss)
    }
}
