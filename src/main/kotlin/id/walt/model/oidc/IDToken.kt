package id.walt.model.oidc

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import id.walt.model.dif.PresentationSubmission
import id.walt.services.jwt.JwtService
import id.walt.vclib.model.VerifiableCredential
import java.time.Instant

data class IDToken(
  @Json("iss") val issuer: String = "https://self-issued.me/v2",
  @Json("sub") val subject: String,
  @Json("aud") val client_id: String,
  @Json("exp") val expiration: Long = Instant.now().plusSeconds(60*60).epochSecond,
  @Json("iat") val issueDate: Long = Instant.now().epochSecond,
  val nonce: String,
  @Json(name = "_vp_token_", serializeNull = false) val vpTokenRef: VpTokenRef?
) {
  fun sign(): String {
    return JwtService.getService().sign(subject, Klaxon().toJsonString(this))
  }
}

data class VpTokenRef (
  val presentation_submission: PresentationSubmission
)
