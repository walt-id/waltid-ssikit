package id.walt.services.jwt

import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import id.walt.crypto.KeyAlgorithm
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.crypto.CryptoService
import id.walt.services.did.DidService
import id.walt.services.key.KeyService
import id.walt.test.RESOURCES_PATH
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import java.util.*
import java.util.stream.*

private const val customClaim = "https://self-issued.me"
private const val defaultClaim = "https://walt.id"

internal class JwtServiceTest {

    @BeforeEach
    fun setup() {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    }

    private val cryptoService = CryptoService.getService()
    private val keyService = KeyService.getService()
    private val jwtService = JwtService.getService()

    @ParameterizedTest
    @MethodSource("generateJwtNoPayloadSource")
    fun generateJwtNoPayload(input: JwtTestInput, expected: Boolean) {
        val keyId = cryptoService.generateKey(input.keyAlgorithm)

        val jwt = jwtService.sign(keyId.id)

        val signedJwt = SignedJWT.parse(jwt)
        input.algorithmName shouldBe signedJwt.header.algorithm.name
        keyId.id shouldBe signedJwt.header.keyID
        signedJwt.jwtClaimsSet.claims["iss"] shouldBe input.claim

        val res1 = jwtService.verify(jwt)
        res1.verified shouldBe expected
    }

    @ParameterizedTest
    @MethodSource("generateJwtCustomPayloadSource")
    fun generateJwtCustomPayload(input: JwtTestInput, expected: Boolean) {
        val did = DidService.create(DidMethod.ebsi, keyService.generate(input.keyAlgorithm).id)
        val kid = DidService.load(did).verificationMethod!![0].id
        val key = keyService.toJwk(did, jwkKeyId = kid)
        val thumbprint = key.computeThumbprint().toString()

        val payload = generatePayload(key, thumbprint)

        val jwtStr = jwtService.sign(kid, payload)
        val jwt = SignedJWT.parse(jwtStr)
        jwt.header.algorithm.name shouldBe input.algorithmName
        jwt.header.keyID shouldBe kid
        jwt.jwtClaimsSet.claims["iss"] shouldBe input.claim
        jwt.jwtClaimsSet.claims["sub"] shouldBe thumbprint

        jwtService.verify(jwtStr).verified shouldBe expected
    }

    private fun generatePayload(key: JWK, thumbprint: String) = JWTClaimsSet.Builder()
        .issuer(customClaim)
        .audience("redirectUri")
        .subject(thumbprint)
        .issueTime(Date.from(Instant.now()))
        .expirationTime(Date.from(Instant.now().plusSeconds(120)))
        .claim("nonce", "nonce")
        .claim("sub_jwk", key.toJSONObject())
        .build().toString()

    companion object {
        @JvmStatic
        fun generateJwtNoPayloadSource(): Stream<Arguments> = Stream.of(
            arguments(
                JwtTestInput(KeyAlgorithm.ECDSA_Secp256k1, "ES256K", defaultClaim),
                true
            ),
            arguments(
                JwtTestInput(KeyAlgorithm.EdDSA_Ed25519, "EdDSA", defaultClaim),
                true
            ),
            arguments(
                JwtTestInput(KeyAlgorithm.RSA, "RS256", defaultClaim),
                true
            ),
        )

        @JvmStatic
        fun generateJwtCustomPayloadSource(): Stream<Arguments> = Stream.of(
            arguments(
                JwtTestInput(KeyAlgorithm.ECDSA_Secp256k1, "ES256K", customClaim),
                true
            ),
            arguments(
                JwtTestInput(KeyAlgorithm.EdDSA_Ed25519, "EdDSA", customClaim),
                true
            ),
            arguments(
                JwtTestInput(KeyAlgorithm.RSA, "RS256", customClaim),
                true
            ),
        )
    }

    data class JwtTestInput(
        val keyAlgorithm: KeyAlgorithm,
        val algorithmName: String,
        val claim: String,

    )
}
