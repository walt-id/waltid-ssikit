package org.letstrust.services.jwt

import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.*
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import mu.KotlinLogging
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.crypto.LdSigner
import org.letstrust.crypto.LetsTrustProvider
import org.letstrust.crypto.PrivateKeyHandle
import org.letstrust.services.key.KeyManagementService
import java.security.interfaces.ECPublicKey
import java.util.*

private val log = KotlinLogging.logger {}

object JwtService {

    // TODO load key and load encryption config based on key-alg
//        val recipientJWK = KeyManagementService.loadKeys(keyAlias)
//        if (recipientJWK == null) {
//            log.error { "Could not load verifying key for $keyAlias" }
//            throw Exception("Could not load verifying key for $keyAlias")
//        }
    val keyId = "123"
    val encKey: OctetKeyPair = OctetKeyPairGenerator(Curve.X25519)
        .keyID(keyId)
        .generate()


    fun encrypt(
        keyAlias: String, // verification method
        payload: String? = null
    ): String {
        //TODO key loading/storing
        val jweObject = JWEObject(
            JWEHeader.Builder(JWEAlgorithm.ECDH_ES, EncryptionMethod.A256GCM)
                .contentType("JWT") // required to indicate nested JWT
                .keyID(keyId)
                .build(),
            Payload(payload)
        )

        val pubEncKey = encKey.toPublicJWK()
        val encrypter = X25519Encrypter(pubEncKey)
        encrypter.jcaContext.provider = LetsTrustProvider()
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
        decrypter.jcaContext.provider = LetsTrustProvider()
        jweObj.decrypt(decrypter)

        return jweObj.payload.toString()
    }

    fun sign(
        keyAlias: String, // verification method
        payload: String? = null
    ): String {

        // Default JWT claims
//        val claimsSet = JWTClaimsSet.Builder()
//            .subject("alice")
//            .issuer("https://c2id.com")
//            .expirationTime(Date(Date().getTime() + 60 * 1000))
//            .build()


        val claimsSet = if (payload != null) JWTClaimsSet.parse(payload) else JWTClaimsSet.Builder()
            .subject(keyAlias)
            .issuer("https://letstrust.org")
            .expirationTime(Date(Date().getTime() + 60 * 1000))
            .build()

        val issuerKey = KeyManagementService.load(keyAlias)
        if (issuerKey == null) {
            log.error { "Could not load signing key for $keyAlias" }
            throw Exception("Could not load signing key for $keyAlias")
        }

        val jwt = when (issuerKey.algorithm) {
            KeyAlgorithm.EdDSA_Ed25519 -> {
                var jwt = SignedJWT(JWSHeader.Builder(JWSAlgorithm.EdDSA).keyID(keyAlias).build(), claimsSet)
                //jwt.sign(Ed25519Signer(issuerKey.toOctetKeyPair()))
                jwt.sign(LdSigner.JwsLtSigner(issuerKey.keyId))
                jwt
            }
            KeyAlgorithm.ECDSA_Secp256k1 -> {
                val jwt = SignedJWT(JWSHeader.Builder(JWSAlgorithm.ES256K).keyID(keyAlias).build(), claimsSet)
                val jwsSigner = ECDSASigner(PrivateKeyHandle(issuerKey.keyId), Curve.SECP256K1)
                jwsSigner.jcaContext.provider = LetsTrustProvider()
                jwt.sign(jwsSigner)
                jwt
            }
            else -> {
                log.error { "Algorithm ${issuerKey.algorithm} not supported" }
                throw Exception("Algorithm ${issuerKey.algorithm} not supported")
            }
        }

        val jwtStr = jwt.serialize()
        log.debug { "Signed JWT:  $jwtStr" }
        return jwtStr
    }

    fun verify(token: String): Boolean {
        log.debug { "Verifying token:  $token" }
        val jwt = SignedJWT.parse(token)

        //TODO: key might also be entirely extracted out of the header",
        // Maybe resolve DID (verification method)
        val verifierKey = KeyManagementService.load(jwt.header.keyID)
        if (verifierKey == null) {
            log.error { "Could not load verifying key for $jwt.header.keyID" }
            throw Exception("Could not load verifying key for $jwt.header.keyID")
        }

        val res = when (verifierKey.algorithm) {
            KeyAlgorithm.EdDSA_Ed25519 -> jwt.verify(Ed25519Verifier(verifierKey.toJwk()))
            KeyAlgorithm.ECDSA_Secp256k1 -> jwt.verify(ECDSAVerifier(verifierKey.getPublicKey() as ECPublicKey))
            else -> {
                log.error { "Algorithm ${verifierKey.algorithm} not supported" }
                throw Exception("Algorithm ${verifierKey.algorithm} not supported")
            }
        }

        log.debug { "JWT verified returned:  $res" }
        return res
    }

    fun parseClaims(token: String): MutableMap<String, Any>? {
        val jwt = SignedJWT.parse(token)
        val claimsMap = jwt.jwtClaimsSet.claims
        return claimsMap
    }
}
