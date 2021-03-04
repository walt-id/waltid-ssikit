package org.letstrust

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.crypto.Ed25519Signer
import com.nimbusds.jose.crypto.Ed25519Verifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import mu.KotlinLogging
import java.util.*

private val log = KotlinLogging.logger {}

object JwtService {

    fun sign(
        keyAlias: String,
        payload: String
    ): String {

        // TODO: replace sample JWT claims
        val claimsSet = JWTClaimsSet.Builder()
            .subject("alice")
            .issuer("https://c2id.com")
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
}
