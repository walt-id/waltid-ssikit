package id.walt.model.oidc

import com.beust.klaxon.JsonObject
import com.nimbusds.jwt.JWTClaimsSet
import id.walt.model.dif.PresentationSubmission
import id.walt.services.jwt.JwtService
import id.walt.services.oidc.OIDCUtils
import id.walt.vclib.credentials.VerifiablePresentation
import id.walt.vclib.model.toCredential
import java.io.StringReader
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

data class SIOPv2Response(
  val vp_token: List<VerifiablePresentation>,
  val presentation_submission: PresentationSubmission,
  val id_token: String?,
  val state: String?
) {

  private fun enc(v: String) = URLEncoder.encode(v, StandardCharsets.UTF_8)

  fun toFormParams(): Map<String, String> {
    val vpTokenString = OIDCUtils.toVpToken(vp_token)
    return buildMap {
      put("vp_token", vpTokenString)
      put("presentation_submission", klaxon.toJsonString(presentation_submission))
      id_token?.let { put("id_token", it) }
      state?.let { put("state", it) }
    }
  }

  fun toFormBody(): String {
    return toFormParams().map { "${enc(it.key)}=${enc(it.value)}" }.joinToString("&")
  }

  fun toEBSIWctJson(): String {
    val idToken = SelfIssuedIDToken.parse(id_token!!)
    return klaxon.toJsonString(
      mapOf("id_token" to id_token, "vp_token" to vp_token.flatMap { vp -> vp.verifiableCredential }.map { vc ->
        mapOf("format" to "jwt_vp", "presentation" to JwtService.getService().sign(idToken!!.subject,
          JWTClaimsSet.Builder().subject(idToken.subject).issuer(idToken.subject).issueTime(Date()).claim("nonce", vp_token.first().challenge).jwtID(vc.id).claim("vc", vc.encode()).build().toString())
        )
    }))
  }

  companion object {
    fun fromFormParams(params: Map<String, String>): SIOPv2Response {
      val vpTokenStr = params["vp_token"] ?: throw Exception("vp_token parameter must be set")
      val presentationSubmissionStr = params["presentation_submission"] ?: throw Exception("presentation_submission parameter must be set")
      return SIOPv2Response(
        vp_token = OIDCUtils.fromVpToken(vpTokenStr),
        presentation_submission = klaxon.parse<PresentationSubmission>(presentationSubmissionStr) ?: throw Exception("Could not parse presentation_submission parameter"),
        id_token = params["id_token"],
        state = params["state"]
      )
    }
  }
}
