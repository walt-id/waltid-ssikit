import com.nimbusds.jose.shaded.json.JSONObject
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.letstrust.JwtService
import org.letstrust.KeyManagementService
import org.letstrust.model.AuthenticationRequestPayload
import org.letstrust.model.AuthenticationResponsePayload
import org.letstrust.model.AuthenticationResponseVerifiedClaims
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JwtServiceTest {

    @Test
    fun genJwtSecp256k1() {
        val keyId = KeyManagementService.generateSecp256k1KeyPairSun()

        val jwt = JwtService.sign(keyId)

        val res1 = JwtService.verify(jwt)
        assertTrue(res1, "JWT verification failed")
    }

    @Test
    fun genJwtEd25519() {
        val keyId = KeyManagementService.generateEd25519KeyPairNimbus()

        val jwt = JwtService.sign(keyId)

        val res1 = JwtService.verify(jwt)
        assertTrue(res1, "JWT verification failed")
    }

    @Test
    fun parseClaimsTest() {

        val token =
            "eyJraWQiOiJMZXRzVHJ1c3QtS2V5LTFhZjNlMWIzNGIxNjQ3NzViNGQwY2IwMDJkODRlZmQwIiwidHlwIjoiSldUIiwiYWxnIjoiRVM1MTIifQ.eyJzdWIiOiIwNTQyNTVhOC1iODJmLTRkZWQtYmQ0OC05NWY5MGY0NmM1M2UiLCJpc3MiOiJodHRwczovL2FwaS5sZXRzdHJ1c3QuaW8iLCJleHAiOjE2MTUyMDg5MTYsImlhdCI6MTYxNTIwNTkxNn0.ARUKAO0f6vpRyUXWWEeL4xPegzl66eaC-AeEXswhsrs1OREae81JPNqnWs8e3rTrRCLCfRTcVS658hV8jfjAAY6vASwtNjV9HwJcmUGmpanBjAuQkJLkmv6Sn3lqzF5PU3hFv3GnVznvcDDyLRlsI8OooPZmM6p-FWUR8tAYKpvzAdMB\n"

        val claims = JwtService.parseClaims(token)!!

        // claims?.iterator()?.forEach { println(it) }

        assertEquals("054255a8-b82f-4ded-bd48-95f90f46c53e", claims["sub"].toString())
        assertEquals("https://api.letstrust.io", claims["iss"].toString())
        assertEquals("Mon Mar 08 14:08:36 CET 2021", claims["exp"].toString())
        assertEquals("Mon Mar 08 13:18:36 CET 2021", claims["iat"].toString())
    }

    @Test
    fun signAuthenticationRequest() {
        val payload = File("src/test/resources/ebsi/authentication-request-payload-dummy.json").readText()

        val arp = Json.decodeFromString<AuthenticationRequestPayload>(payload)

        val keyId = KeyManagementService.generateSecp256k1KeyPairSun()

        val jwt = JwtService.sign(keyId, Json.encodeToString(arp))

        println(jwt)

        val claims = JwtService.parseClaims(jwt)

        assertEquals("openid did_authn", claims?.get("scope")!!.toString())

        val res = JwtService.verify(jwt)

        assertTrue(res, "JWT verification failed")
    }


    //    {
//        "iss": "did:ebsi:0x123abc",
//        "sub": "{thumbprint of the sub_jwk}",
//        "aud": "did:ebsi:RP-did-here", => WHICH DID IS THIS???
//        "iat": 1610714000,
//        "exp": 1610714900,
//        "sub_jwk":{signing JWK},
//        "sub_did_verification_method_uri": "did:ebsi:0x123abc#authentication-key-proof-3",
//        "nonce": "n-0S6_WzA2M",
//        "claims": {
//        "verified_claims": {Authentication-Response-Verifiable-Presentation},
//        "encryption_key": {JWK encryption key}
//    }
//    }
    @Test
    fun signAuthenticationResponse() {
        val vp = File("src/test/resources/ebsi/authentication-response-verifiable-presentation.json").readText()

        val arp = AuthenticationResponsePayload(
            "did:ebsi:0x123abc",
            "thumbprint of the sub_jwk",
            "did:ebsi:RP-did-here",
            1610714000,
            1610714900,
            "signing JWK",
            "did:ebsi:0x123abc#authentication-key-proof-3",
            "n-0S6_WzA2M",
            AuthenticationResponseVerifiedClaims("vp", "enc_key")
        )

        val keyId = KeyManagementService.generateSecp256k1KeyPairSun()

        val jwt = JwtService.sign(keyId, Json.encodeToString(arp))

        println(jwt)

        val claims = JwtService.parseClaims(jwt)!!

        val childClaims = claims["claims"] as JSONObject

        assertEquals("vp", childClaims["verified_claims"])

        val res = JwtService.verify(jwt)

        assertTrue(res, "JWT verification failed")
    }

}
