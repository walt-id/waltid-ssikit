package id.walt.services.jwt

import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.*
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import id.walt.crypto.*
import id.walt.model.DidUrl
import id.walt.services.did.DidService
import id.walt.services.key.KeyService
import mu.KotlinLogging
import java.security.Provider
import java.security.interfaces.ECPublicKey
import java.util.*

val keyId = "123" // FIXME static keyId

open class WaltIdJwtService : JwtService() {

    private val log = KotlinLogging.logger {}

    val encKey: OctetKeyPair = OctetKeyPairGenerator(Curve.X25519).keyID(keyId).generate()

    open val keyService = KeyService.getService()
    open val provider: Provider = WaltIdProvider()

    override fun encrypt(
        kid: String, pubEncKey: OctetKeyPair, payload: String?
    ): String {

        val jweObject = JWEObject(
            JWEHeader.Builder(JWEAlgorithm.ECDH_ES, EncryptionMethod.A256GCM)
                .contentType("JWT") // required to indicate nested JWT
                .keyID(kid).build(), Payload(payload)
        )

        val pubEncKeyJwt = pubEncKey.toPublicJWK()
        val encrypter = X25519Encrypter(pubEncKeyJwt)
        // encrypter.jcaContext.provider = waltIdProvider
        jweObject.encrypt(encrypter)
        return jweObject.serialize()
    }

    fun decrypt(
        jwe: String
    ): String {
        val jweObj = JWEObject.parse(jwe)

        val keyId = jweObj.header.keyID

        //TODO: key loading/storing
        //val encKey = KeyManagementService.load(keyId)
        if (encKey == null) {
            log.error { "Could not load verifying key for $keyId" }
            throw Exception("Could not load verifying key for $keyId")
        }
        val decrypter = X25519Decrypter(encKey)
        decrypter.jcaContext.provider = provider
        jweObj.decrypt(decrypter)

        return jweObj.payload.toString()
    }

    private fun createSignedJwt(jwsAlgorithm: JWSAlgorithm, keyAlias: String, claimsSet: JWTClaimsSet, includeJwk: JWK?) =
        SignedJWT(
            JWSHeader
                .Builder(jwsAlgorithm)
                .keyID(keyAlias)
                .type(JOSEObjectType.JWT)
                .apply {
                    includeJwk?.let { jwk(it) }
                }.build(),
            claimsSet
        )/*.also {
            // log.debug { "Created signable JWT object: $it." }
        }*/


    override fun sign(
        keyAlias: String, // verification method
        payload: String?
    ): String {

        // Default JWT claims
//        val claimsSet = JWTClaimsSet.Builder()
//            .subject("alice")
//            .issuer("https://c2id.com")
//            .expirationTime(Date(Date().getTime() + 60 * 1000))
//            .build()


        val claimsSet = if (payload != null) JWTClaimsSet.parse(payload) else JWTClaimsSet.Builder().subject(keyAlias)
            .issuer("https://walt.id").expirationTime(Date(Date().time + 60 * 1000)).build()

        val issuerKey = keyService.load(keyAlias)
        if (issuerKey == null) {
            log.error { "Could not load signing key for $keyAlias" }
            throw Exception("Could not load signing key for $keyAlias")
        }

        log.debug { "Signing JWT with algorithm: ${issuerKey.algorithm}" }
        val includeJwk: JWK? = if (DidService.isDidEbsiV2(keyAlias)) {
            keyService.toJwk(keyAlias).toPublicJWK()
        } else {
            null
        }

        val signedJwt = when (issuerKey.algorithm) {
            KeyAlgorithm.EdDSA_Ed25519 -> {
                val jwt = createSignedJwt(JWSAlgorithm.EdDSA, keyAlias, claimsSet, includeJwk)

                //jwt.sign(Ed25519Signer(issuerKey.toOctetKeyPair()))
                jwt.sign(LdSigner.JwsLtSigner(issuerKey.keyId))
                jwt
            }

            KeyAlgorithm.ECDSA_Secp256k1 -> {
                val jwt = createSignedJwt(JWSAlgorithm.ES256K, keyAlias, claimsSet, includeJwk)

                val jwsSigner = ECDSASigner(ECPrivateKeyHandle(issuerKey.keyId), Curve.SECP256K1)
                jwsSigner.jcaContext.provider = provider
                jwt.sign(jwsSigner)
                jwt
            }

            KeyAlgorithm.ECDSA_Secp256r1 -> {
                val jwt = createSignedJwt(JWSAlgorithm.ES256, keyAlias, claimsSet, includeJwk)

                val jwsSigner = ECDSASigner(ECPrivateKeyHandle(issuerKey.keyId), Curve.P_256)
                jwsSigner.jcaContext.provider = provider
                jwt.sign(jwsSigner)
                jwt
            }

            else -> {
                log.error { "Algorithm ${issuerKey.algorithm} not supported" }
                throw Exception("Algorithm ${issuerKey.algorithm} not supported")
            }
        }

        val serializedSignedJwt = signedJwt.serialize()
        log.debug { "Signed JWT:  $serializedSignedJwt" }
        return serializedSignedJwt
    }

    override fun verify(token: String): Boolean {
        log.debug { "Verifying token:  $token" }
        val jwt = SignedJWT.parse(token)
        val issuer = jwt.jwtClaimsSet.issuer
        val keyAlias = jwt.header.keyID.orEmpty().ifEmpty { issuer }
        if (DidUrl.isDidUrl(keyAlias)) { // issuer is a valid DID
            if (DidService.isDidEbsiV2(keyAlias) && jwt.header.jwk != null) {
                DidService.importKeyForDidEbsiV2(DidUrl.from(keyAlias).did, jwt.header.jwk)
            } else if (!DidService.importKeys(DidUrl.from(keyAlias).did)) {
                throw Exception("Could not resolve verification keys")
            }
        }

        val verifierKey = keyService.load(keyAlias)

        val res = when (verifierKey.algorithm) {
            KeyAlgorithm.EdDSA_Ed25519 -> jwt.verify(Ed25519Verifier(keyService.toEd25519Jwk(verifierKey)))
            KeyAlgorithm.ECDSA_Secp256k1 -> {
                val verifier = ECDSAVerifier(PublicKeyHandle(verifierKey.keyId, verifierKey.getPublicKey() as ECPublicKey))
                verifier.jcaContext.provider = provider
                jwt.verify(verifier)
            }

            KeyAlgorithm.ECDSA_Secp256r1 -> {
                val verifier = ECDSAVerifier(PublicKeyHandle(verifierKey.keyId, verifierKey.getPublicKey() as ECPublicKey))
                verifier.jcaContext.provider = provider
                jwt.verify(verifier)
            }

            else -> {
                log.error { "Algorithm ${verifierKey.algorithm} not supported" }
                throw Exception("Algorithm ${verifierKey.algorithm} not supported")
            }
        }

        log.debug { "JWT verified returned:  $res" }
        return res
    }

    override fun parseClaims(token: String): MutableMap<String, Any>? {
        val jwt = SignedJWT.parse(token)
        val claimsMap = jwt.jwtClaimsSet.claims
        return claimsMap
    }

}
