package id.walt.model.oidc

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import id.walt.services.did.DidService
import id.walt.services.jwt.JwtService
import id.walt.services.keystore.KeyStoreService
import java.time.Instant
import java.util.*

data class SelfIssuedIDToken(
    val subject: String,
    val client_id: String?,
    val nonce: String?,
    val expiration: Instant?,
    val issueDate: Instant? = Instant.now(),
    // legacy OIDC4VP spec, including presentation submission in _vp_token JWT claim of id_token (used by EBSI conformance v2)
    val _vp_token: VpTokenRef? = null
) {
    fun sign(): String {
        val builder = JWTClaimsSet.Builder().subject(subject).issuer(subject)
        client_id?.let { builder.audience(it) }
        nonce?.let { builder.claim("nonce", it) }
        expiration?.let { builder.expirationTime(Date.from(expiration)) }
        issueDate?.let { builder.issueTime(Date.from(issueDate)) }
        _vp_token?.let { builder.claim("_vp_token", it) }

        return JwtService.getService().sign(
            subject,
            builder.build().toString()
        )
    }

    companion object {
        fun parse(jwt: String): SelfIssuedIDToken? {
            val parsedJWT = SignedJWT.parse(jwt) ?: return null
            return SelfIssuedIDToken(
                subject = parsedJWT.jwtClaimsSet.subject,
                client_id = parsedJWT.jwtClaimsSet.audience?.firstOrNull(),
                nonce = parsedJWT.jwtClaimsSet.getStringClaim("nonce"),
                expiration = parsedJWT.jwtClaimsSet.expirationTime?.toInstant(),
                issueDate = parsedJWT.jwtClaimsSet.issueTime?.toInstant()
            )
        }

        fun verify(jwt: String): Boolean {
            val parsedToken = parse(jwt) ?: return false
            if (KeyStoreService.getService().getKeyId(parsedToken.subject) == null) {
                DidService.importKeys(parsedToken.subject)
            }
            return JwtService.getService().verify(jwt).verified
        }
    }
}
