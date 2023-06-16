package id.walt.services.jwt

import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import id.walt.crypto.KeyAlgorithm
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.crypto.CryptoService
import id.walt.services.did.DidService
import id.walt.services.key.KeyService
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.*

class JwtServiceTest : AnnotationSpec() {

    @Before
    fun setup() {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    }

    private val cryptoService = CryptoService.getService()
    private val keyService = KeyService.getService()
    private val jwtService = JwtService.getService()

    @Test
    fun genJwtSecp256k1() {
        val keyId = cryptoService.generateKey(KeyAlgorithm.ECDSA_Secp256k1)

        val jwt = jwtService.sign(keyId.id)

        val signedJwt = SignedJWT.parse(jwt)
        "ES256K" shouldBe signedJwt.header.algorithm.name
        keyId.id shouldBe signedJwt.header.keyID
        "https://walt.id" shouldBe signedJwt.jwtClaimsSet.claims["iss"]

        val res1 = jwtService.verify(jwt)
        res1.verified shouldBe true
    }

    @Test
    fun genJwtEd25519() {
        val keyId = cryptoService.generateKey(KeyAlgorithm.EdDSA_Ed25519)

        val jwt = jwtService.sign(keyId.id)

        val signedJwt = SignedJWT.parse(jwt)
        "EdDSA" shouldBe signedJwt.header.algorithm.name
        keyId.id shouldBe signedJwt.header.keyID
        "https://walt.id" shouldBe signedJwt.jwtClaimsSet.claims["iss"]

        val res1 = jwtService.verify(jwt)
        res1.verified shouldBe true
    }

    @Test
    fun genJwtCustomPayload() {
        val did = DidService.create(DidMethod.ebsi, keyService.generate(KeyAlgorithm.ECDSA_Secp256k1).id)
        val kid = DidService.load(did).verificationMethod!![0].id
        val key = keyService.toJwk(did, jwkKeyId = kid) as ECKey
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

        val jwtStr = jwtService.sign(kid, payload)
        val jwt = SignedJWT.parse(jwtStr)
        "ES256K" shouldBe jwt.header.algorithm.name
        kid shouldBe jwt.header.keyID
        "https://self-issued.me" shouldBe jwt.jwtClaimsSet.claims["iss"]
        thumbprint shouldBe jwt.jwtClaimsSet.claims["sub"]

        jwtService.verify(jwtStr).verified shouldBe true
    }

}
