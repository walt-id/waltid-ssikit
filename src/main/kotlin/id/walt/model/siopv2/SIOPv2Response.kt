package id.walt.model.siopv2

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import id.walt.services.jwt.JwtService
import id.walt.vclib.VcLibManager
import java.time.Instant

data class SIOPv2Response (
  val did: String,
  val id_token: SIOPv2IDToken,
  val vp_token: SIOPv2VPToken
    ) {
  fun getIdToken(): String {
    return JwtService.getService().sign(did, Klaxon().toJsonString(id_token))
  }

  fun getVpToken(): String {
    return JwtService.getService().sign(did, Klaxon().toJsonString(vp_token))
  }
}

data class SIOPv2IDToken(
  @Json("iss") val issuer: String = "https://self-issued.me/v2",
  @Json("sub") val subject: String,
  @Json("aud") val client_id: String,
  @Json("exp") val expiration: Long = Instant.now().plusSeconds(60*60).epochSecond,
  @Json("iat") val issueDate: Long = Instant.now().epochSecond,
  val nonce: String)

data class SIOPv2VPToken(
  val vp_token: List<SIOPv2Presentation>
)

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
