package id.walt.model.oidc

import com.beust.klaxon.Json
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.nimbusds.jwt.SignedJWT
import id.walt.model.dif.PresentationSubmission
import id.walt.services.did.DidService
import id.walt.services.jwt.JwtService
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyStoreService
import java.time.Instant

data class IDToken(
  @Json("iss") val issuer: String = "https://self-issued.me/v2",
  @Json("sub") val subject: String,
  @Json("aud") val client_id: String,
  @Json("exp") val expiration: Long = Instant.now().plusSeconds((60 * 60).toLong()).epochSecond,
  @Json("iat") val issueDate: Long = Instant.now().epochSecond,
  val nonce: String,
  @Json(name = "_vp_token", serializeNull = false) val vpTokenRef: VpTokenRef?,
  @Json(ignored = true) var jwt: String? = null
) {
    fun sign(): String {
        return JwtService.getService().sign(subject, Klaxon().toJsonString(this))
    }

    fun verify(): Boolean {
      if(jwt != null) {
        if (KeyStoreService.getService().getKeyId(subject) == null) {
          DidService.importKey(subject)
        }
        return JwtService.getService().verify(jwt!!)
      }
      return false
    }

  companion object {
    fun parse(jwt: String): IDToken? {
      return SignedJWT.parse(jwt).jwtClaimsSet.toString().let { klaxon.parse<IDToken>(it) }?.also {
        it.jwt = jwt
      }
    }
  }
}

data class VpTokenRef(
    val presentation_submission: PresentationSubmission
)
