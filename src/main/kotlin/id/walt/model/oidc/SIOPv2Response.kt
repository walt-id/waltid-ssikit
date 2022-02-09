package id.walt.model.oidc

import com.nimbusds.jwt.JWTClaimsSet
import id.walt.services.jwt.JwtService
import id.walt.services.vc.JwtCredentialService
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.vclib.credentials.VerifiablePresentation
import net.minidev.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

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

  fun toEBSIWctJson(): String {
    return klaxon.toJsonString(
      mapOf("id_token" to id_token.sign(), "vp_token" to vp_token.flatMap { vp -> vp.verifiableCredential }.map { vc ->
        mapOf("format" to "jwt_vp", "presentation" to JwtService.getService().sign(id_token.subject,
          JWTClaimsSet.Builder().subject(id_token.subject).issuer(id_token.subject).issueTime(Date()).claim("nonce", vp_token.first().challenge).jwtID(vc.id).claim("vc", vc.encode()).build().toString())
        )
    }))
  }
}
