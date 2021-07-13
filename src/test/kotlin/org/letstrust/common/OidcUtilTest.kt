package org.letstrust.common

import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class OidcUtilTest {

    @Test
    fun authenticationRequestTest() {

        val kidServer = "22df3f6e54494c12bfb559e171cfe747"
        val didServer = "did:ebsi:0x416e6e6162656c2e4c65652e452d412d506f652e"
        val redirectUri = "http://localhost:8080/redirect"
        val callback = "http://localhost:8080/callback"
        val nonce = UUID.randomUUID().toString()

        val oidcReq = OidcUtil.generateOidcAuthenticationRequest(kidServer, didServer, redirectUri, callback, nonce)

        val didAuthReq = OidcUtil.validateOidcAuthenticationRequest(oidcReq)

        assertEquals(callback, didAuthReq.callback)
        assertEquals(nonce, didAuthReq.nonce)
        assertEquals(redirectUri, didAuthReq.client_id)
        assertEquals(redirectUri, didAuthReq.authenticationRequestJwt.authRequestPayload.client_id)
        assertEquals(didServer, didAuthReq.authenticationRequestJwt.authRequestPayload.iss)
    }
}
