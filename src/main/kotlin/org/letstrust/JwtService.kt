package org.letstrust

import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.*
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import mu.KotlinLogging
import java.util.*

private val log = KotlinLogging.logger {}

object JwtService {

    fun encrypt(
        keyAlias: String,
        payload: String? = null
    ): String {

// TODO load key and load encryption config based on key-alg
//        val recipientJWK = KeyManagementService.loadKeys(keyAlias)
//        if (recipientJWK == null) {
//            log.error { "Could not load verifying key for $keyAlias" }
//            throw Exception("Could not load verifying key for $keyAlias")
//        }
        val encKey = OctetKeyPairGenerator(Curve.X25519)
            .keyID("123")
            .generate()
        val pubEncKey = encKey.toPublicJWK()


        val jweObject = JWEObject(
            JWEHeader.Builder(JWEAlgorithm.ECDH_ES, EncryptionMethod.A256GCM)
                .contentType("JWT") // required to indicate nested JWT
                .build(),
            Payload(payload)
        )

        jweObject.encrypt(X25519Encrypter(pubEncKey))
        return jweObject.serialize()
    }

    fun dencrypt(
        jwe: String
    ): String {
        val jweObj = JWEObject.parse(jwe)

        val keyId = jweObj.header.keyID

        val encKey = KeyManagementService.loadKeys(keyId)
        if (encKey == null) {
            log.error { "Could not load verifying key for $keyId" }
            throw Exception("Could not load verifying key for $keyId")
        }
        jweObj.decrypt(X25519Decrypter(encKey.toOctetKeyPair()))

        return jweObj.payload.toString()
    }

    fun sign(
        keyAlias: String,
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

        val issuerKey = KeyManagementService.loadKeys(keyAlias)
        if (issuerKey == null) {
            log.error { "Could not load signing key for $keyAlias" }
            throw Exception("Could not load signing key for $keyAlias")
        }

        val jwt = when (issuerKey.algorithm) {
            "Ed25519" -> {
                var jwt = SignedJWT(JWSHeader.Builder(JWSAlgorithm.EdDSA).keyID(keyAlias).build(), claimsSet)
                jwt.sign(Ed25519Signer(issuerKey.toOctetKeyPair()))
                jwt
            }
            "EC" -> {
                val jwt = SignedJWT(JWSHeader.Builder(JWSAlgorithm.ES256K).keyID(keyAlias).build(), claimsSet)
                jwt.sign(ECDSASigner(issuerKey.toEcKey()))
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
        val verifierKey = KeyManagementService.loadKeys(jwt.header.keyID)
        if (verifierKey == null) {
            log.error { "Could not load verifying key for $jwt.header.keyID" }
            throw Exception("Could not load verifying key for $jwt.header.keyID")
        }

        val res = when (verifierKey.algorithm) {
            "Ed25519" -> jwt.verify(Ed25519Verifier(verifierKey.toOctetKeyPair().toPublicJWK()))
            "EC" -> jwt.verify(ECDSAVerifier(verifierKey.toEcKey()))
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
