package org.letstrust.services.jwt

import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.shaded.json.JSONObject
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.letstrust.LetsTrustServices
import org.letstrust.crypto.CryptoService
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.model.*
import org.letstrust.services.did.DidService
import org.letstrust.services.key.KeyService
import org.letstrust.services.vc.CredentialService
import java.io.File
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JwtServiceTest {

    val cs = LetsTrustServices.load<CryptoService>()


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
    fun genJwtSecp256k1() {
        val keyId = cs.generateKey(KeyAlgorithm.ECDSA_Secp256k1)

        val jwt = JwtService.sign(keyId.id)

        val signedJwt = SignedJWT.parse(jwt)
        assertEquals("ES256K", signedJwt.header.algorithm.name)
        assertEquals(keyId.id, signedJwt.header.keyID)
        assertEquals("https://letstrust.org", signedJwt.jwtClaimsSet.claims["iss"])

        val res1 = JwtService.verify(jwt)
        assertTrue(res1, "JWT verification failed")
    }

    @Test
    fun genJwtCustomPayload() {
        val did = DidService.create(DidMethod.ebsi, KeyService.generate(KeyAlgorithm.ECDSA_Secp256k1).id)
        val kid = "$did#key-1"
        val key = KeyService.toJwk(did, false, kid) as ECKey
        val thumbprint = key.computeThumbprint().toString()

        val payload = JWTClaimsSet.Builder()
            .issuer("https://self-issued.me")
            .audience("redirectUri")
            .subject(thumbprint)
            .issueTime(Date.from(Instant.now()))
            .expirationTime(Date.from(Instant.now().plusSeconds(120)))
            .claim("nonce", "nonce")
            .claim("sub_jwk", key.toJSONObject())
            .build().toString()

        val jwtStr = JwtService.sign(kid, payload)
        val jwt = SignedJWT.parse(jwtStr)
        assertEquals("ES256K", jwt.header.algorithm.name)
        assertEquals(kid, jwt.header.keyID)
        assertEquals("https://self-issued.me", jwt.jwtClaimsSet.claims["iss"])
        assertEquals(thumbprint, jwt.jwtClaimsSet.claims["sub"])

        assertTrue(JwtService.verify(jwtStr), "JWT verification failed")
    }

    @Test
    fun genJwtEd25519() {
        val keyId = cs.generateKey(KeyAlgorithm.EdDSA_Ed25519)

        val jwt = JwtService.sign(keyId.id)

        val signedJwt = SignedJWT.parse(jwt)
        assertEquals("EdDSA", signedJwt.header.algorithm.name)
        assertEquals(keyId.id, signedJwt.header.keyID)
        assertEquals("https://letstrust.org", signedJwt.jwtClaimsSet.claims["iss"])

        val res1 = JwtService.verify(jwt)
        assertTrue(res1, "JWT verification failed")
    }

    @Test
    fun signAuthenticationRequest() {
        val payload = File("src/test/resources/ebsi/authentication-request-payload-dummy.json").readText()

        val arp = Json.decodeFromString<AuthenticationRequestPayload>(payload)

        val keyId = KeyService.generate(KeyAlgorithm.ECDSA_Secp256k1)

        val jwt = JwtService.sign(keyId.id, Json.encodeToString(arp))

        println(jwt)

        val claims = JwtService.parseClaims(jwt)

        assertEquals("openid did_authn", claims?.get("scope")!!.toString())

        val res = JwtService.verify(jwt)

        assertTrue(res, "JWT verification failed")
    }

    // This test-case depends on the associated subject-key in verifiable-authorization2.json, which needs to be available in the keystore
    //@Test
    fun signAuthenticationResponseTest() {
        val verifiableAuthorization = File("src/test/resources/ebsi/verifiable-authorization2.json").readText()

        val vp = CredentialService.present(verifiableAuthorization, "api.ebsi.xyz", null)

        val arp = AuthenticationResponsePayload(
            "did:ebsi:0x123abc",
            "thumbprint of the sub_jwk",
            "did:ebsi:RP-did-here",
            1610714000,
            1610714900,
            "signing JWK",
            "did:ebsi:0x123abc#authentication-key-proof-3",
            "n-0S6_WzA2M",
            AuthenticationResponseVerifiedClaims(vp, "enc_key")
        )

        println(Json { prettyPrint = true }.encodeToString(arp))

        val keyId = KeyService.generate(KeyAlgorithm.ECDSA_Secp256k1)

        val jwt = JwtService.sign(keyId.id, Json.encodeToString(arp))

        println(jwt)

        val resJwt = JwtService.verify(jwt)

        assertTrue(resJwt, "JWT verification failed")

        val claims = JwtService.parseClaims(jwt)!!

        val childClaims = claims["claims"] as JSONObject

        assertEquals(vp, childClaims["verified_claims"])

        val resVP = CredentialService.verifyVp(vp)

        assertTrue(resVP, "LD-Proof verification failed")

    }

    // https://ec.europa.eu/cefdigital/wiki/display/BLOCKCHAININT/Authorisation+API
    @Test
    fun authenticatedKeyExchangeTest() {
        val accessToken =
            "eyJraWQiOiJMZXRzVHJ1c3QtS2V5LTFhZjNlMWIzNGIxNjQ3NzViNGQwY2IwMDJkODRlZmQwIiwidHlwIjoiSldUIiwiYWxnIjoiRVM1MTIifQ.eyJzdWIiOiIwNTQyNTVhOC1iODJmLTRkZWQtYmQ0OC05NWY5MGY0NmM1M2UiLCJpc3MiOiJodHRwczovL2FwaS5sZXRzdHJ1c3QuaW8iLCJleHAiOjE2MTUyMDg5MTYsImlhdCI6MTYxNTIwNTkxNn0.ARUKAO0f6vpRyUXWWEeL4xPegzl66eaC-AeEXswhsrs1OREae81JPNqnWs8e3rTrRCLCfRTcVS658hV8jfjAAY6vASwtNjV9HwJcmUGmpanBjAuQkJLkmv6Sn3lqzF5PU3hFv3GnVznvcDDyLRlsI8OooPZmM6p-FWUR8tAYKpvzAdMB"

        val did_of_rp = DidService.create(DidMethod.key) // Creates a Ed25519 key, as well as an derived X25519 key
        val did_of_client = DidService.create(DidMethod.key)

        // ake1_enc_payload(Access Token, DID(Q)) and encrypts it: c = Enc(Access Token, DID(Q)) -> https://connect2id.com/products/nimbus-jose-jwt/algorithm-selection-guide#encryption
        val ake1_enc_payload = Json { prettyPrint = true }.encodeToString(Ake1EncPayload(accessToken, did_of_rp))
        val ake1_enc_payload_ENC = JwtService.encrypt(did_of_rp, ake1_enc_payload)

        // ake1_sig_payload(nonce, ake1_enc_payload, did(P))
        val ake1_nonce = UUID.randomUUID().toString()
        val ake1_jws_detached = Json { prettyPrint = true }.encodeToString(Ake1JwsDetached(ake1_nonce, ake1_enc_payload_ENC, did_of_client))
        val ake1_jws_detached_SIG = JwtService.sign(did_of_rp, ake1_jws_detached)

        // AKE response (ake1_jws_detached, ake1_enc_payload, did(Q))
        val access_token_response = Json { prettyPrint = true }.encodeToString(AccessTokenResponse(ake1_enc_payload_ENC, ake1_jws_detached_SIG, did_of_rp))

        println("access_token_response:\n" + access_token_response)
        println("ake1_enc_payload:\n" + ake1_enc_payload)
        println("ake1_jws_detached:\n" + ake1_jws_detached)


        // Received AccessTokenResponse
        val received_access_token_response = Json.decodeFromString<AccessTokenResponse>(access_token_response)
        // Verifies the signature ake1_sig_payload
        val verified = JwtService.verify(received_access_token_response.ake1_jws_detached)
        assertTrue(verified)

        // encrypted payload ake1_enc_payload
        val received_ake1_enc_payload = JwtService.decrypt(received_access_token_response.ake1_enc_payload)
        val received_ake1_enc_payload_obj = Json.decodeFromString<Ake1EncPayload>(received_ake1_enc_payload)
        val received_access_token = received_ake1_enc_payload_obj.access_token

        assertEquals(accessToken, received_access_token)
        // Creates an ake1_sig_payload(nonce, ake1_enc_payload, did(P))

    }

}
