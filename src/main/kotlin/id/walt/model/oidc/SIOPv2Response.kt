package id.walt.model.oidc

import id.walt.vclib.credentials.VerifiablePresentation
import net.minidev.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class SIOPv2Response(
  val id_token: IDToken,
  val vp_token: List<VerifiablePresentation>
) {

  private fun enc(v: String) = URLEncoder.encode(v, StandardCharsets.UTF_8)

  fun toFormParams(): Map<String, String> {
    val vpTokenString = when(vp_token.size) {
      1 -> vp_token[0].encode()
      else -> {
        vp_token.map { vp ->
          vp.jwt?.let { "\"it\"" } ?: vp.encode()
        }.joinToString(",", "[", "]")
      }
    }
    return mapOf("id_token" to id_token.sign(), "vp_token" to vpTokenString)
  }

  fun toFormBody(): String {
    return toFormParams().map { "${enc(it.key)}=${enc(it.value)}" }.joinToString("&")
  }

  fun toLegacyJson(): String {
    return klaxon.toJsonString(
      mapOf("id_token" to id_token.sign(), "vp_token" to vp_token.map {
        mapOf("format" to (it.jwt?.let { "jwt_vp" } ?: "ldp_vp"), "presentation" to (it.jwt ?: it.toMap()))
    }))
  }
}
