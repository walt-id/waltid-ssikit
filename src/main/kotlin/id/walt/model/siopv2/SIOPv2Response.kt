package id.walt.model.siopv2

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import id.walt.services.jwt.JwtService
import id.walt.vclib.model.VerifiableCredential
import java.time.Instant

data class SIOPv2IDToken(
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

data class SIOPv2Presentation(
  val format: String,
  val presentation: String
) {
  companion object {
    fun createFromVPString(vpStr: String): SIOPv2Presentation {
      return SIOPv2Presentation(
        format = when(VerifiableCredential.isJWT(vpStr)) {
        true -> "jwt_vp"
        else -> "ldp_vp"
      },
      presentation = vpStr
      )
    }
  }
}

data class VpTokenRef (
  val presentation_submission: PresentationSubmission
)

data class PresentationSubmission (
  val id: String,
  val definition_id: String,
  val descriptor_map: List<PresentationDescriptor>
)

data class PresentationDescriptor (
  @Json(serializeNull = false) val id: String?,
  val format: String,
  val path: String,
  @Json(serializeNull = false) val path_nested: PresentationDescriptor?
) {
  companion object {
    fun fromVP(id: String, vpStr: String) = PresentationDescriptor(
      id = id,
      format = when(VerifiableCredential.isJWT(vpStr)) {
        true -> "jwt_vp"
        else -> "ldp_vp"
      },
      path = "$",
      path_nested = null
    )
  }
}
