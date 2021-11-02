package id.walt.model.siopv2

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import id.walt.services.did.DidService
import id.walt.services.jwt.JwtService
import id.walt.services.vc.VcUtils
import id.walt.vclib.Helpers.encode
import id.walt.vclib.VcLibManager
import id.walt.vclib.vclist.VerifiablePresentation
import java.time.Instant
import java.time.temporal.Temporal
import java.util.*

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
        format = when(VcLibManager.isJWT(vpStr)) {
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
      format = when(VcLibManager.isJWT(vpStr)) {
        true -> "jwt_vp"
        else -> "ldp_vp"
      },
      path = "$",
      path_nested = null
    )
  }
}