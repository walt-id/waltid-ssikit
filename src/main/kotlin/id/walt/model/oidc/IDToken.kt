package id.walt.model.oidc

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import com.nimbusds.jwt.SignedJWT
import id.walt.common.KlaxonWithConverters
import id.walt.model.dif.PresentationSubmission
import id.walt.services.did.DidService
import id.walt.services.jwt.JwtService
import id.walt.services.keystore.KeyStoreService

data class IDToken(
    @Json("iss") val issuer: String = "https://self-issued.me/v2",
    @Json(name = "sub", serializeNull = false) val subject: String? = null,
    @Json(name = "aud", serializeNull = false) val client_id: String? = null,
    @Json(name = "exp", serializeNull = false) val expiration: Long? = null,
    @Json(name = "iat", serializeNull = false) val issueDate: Long? = null,
    @Json(name = "nonce", serializeNull = false) val nonce: String? = null,
    @Json(name = "_vp_token", serializeNull = false) val vpTokenRef: VpTokenRef?,
    @Json(ignored = true) var jwt: String? = null
) {
    fun sign(): String {
        return subject?.let {JwtService.getService().sign(it, Klaxon().toJsonString(this)) } ?:
            throw IllegalArgumentException("No subject specified")
    }

    fun verify(): Boolean {
        if (jwt != null && subject != null) {
            if (KeyStoreService.getService().getKeyId(subject) == null) {
                DidService.importKeys(subject)
            }
            return JwtService.getService().verify(jwt!!).verified
        }
        return false
    }

    companion object {
        fun parse(jwt: String): IDToken? {
            return SignedJWT.parse(jwt).jwtClaimsSet.toString().let { KlaxonWithConverters().parse<IDToken>(it) }?.also {
                it.jwt = jwt
            }
        }
    }
}

data class VpTokenRef(
    val presentation_submission: PresentationSubmission
)
