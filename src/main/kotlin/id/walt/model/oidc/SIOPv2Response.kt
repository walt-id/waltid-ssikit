package id.walt.model.oidc

import com.beust.klaxon.JsonObject
import com.nimbusds.jwt.JWTClaimsSet
import id.walt.services.jwt.JwtService
import id.walt.services.oidc.OIDCUtils
import id.walt.vclib.credentials.VerifiablePresentation
import id.walt.vclib.model.toCredential
import java.io.StringReader
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

data class SIOPv2Response(
  val id_token: IDToken,
  val vp_token: List<VerifiablePresentation>,
  val state: String?
) {

  private fun enc(v: String) = URLEncoder.encode(v, StandardCharsets.UTF_8)

  fun toFormParams(): Map<String, String> {
    val vpTokenString = OIDCUtils.toVpToken(vp_token)
    return mapOf("id_token" to id_token.sign(), "vp_token" to vpTokenString, "state" to (state ?: ""))
  }

  fun toFormBody(): String {
    return toFormParams().map { "${enc(it.key)}=${enc(it.value)}" }.joinToString("&")
  }

  fun toEBSIWctJson(): String {
    return klaxon.toJsonString(
      mapOf("id_token" to id_token.sign(), "vp_token" to vp_token.flatMap { vp -> vp.verifiableCredential }.map { vc ->
        mapOf("format" to "jwt_vp", "presentation" to JwtService.getService().sign(id_token.subject,
          JWTClaimsSet.Builder().subject(id_token.subject).issuer(id_token.subject).issueTime(Date()).claim("nonce", vp_token.first().challenge).jwtID(vc.id).claim("vc", vc.encode()).build().toString())
        )
    }))
  }

  companion object {
    fun fromFormParams(params: Map<String, String>): SIOPv2Response? {
      if(params.containsKey("id_token") && params.containsKey("vp_token")) {
        val idTokenObj = IDToken.parse(params["id_token"]!!)
        val vpTokenStr = params["vp_token"]!!
        if(idTokenObj != null) {
          return SIOPv2Response(
            id_token = idTokenObj,
            vp_token = when(vpTokenStr.startsWith("[")) {
              true -> klaxon.parseJsonArray(StringReader(vpTokenStr))
                .map {
                  when (it) {
                    is JsonObject -> it.toJsonString().toCredential()
                    else -> it.toString().toCredential()
                  } as VerifiablePresentation
                }
              else -> listOf(vpTokenStr.toCredential() as VerifiablePresentation)
            },
            state = params["state"]
          )
        }
      }
      return null
    }
  }
}
