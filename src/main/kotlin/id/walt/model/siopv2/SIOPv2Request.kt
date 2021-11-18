package id.walt.model.siopv2

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import io.javalin.http.Context
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class SIOPv2Request(
  val response_type: String = "id_token",
  val client_id: String,
  val redirect_uri: String,
  val response_mode: String = "fragment",
  val scope: String = "openid",
  val nonce: String,
  val registration: Registration = Registration(),
  @Json("exp") val expiration: Long,
  @Json("iat") val issuedAt: Long,
  val claims: Claims

  ) {
  private fun enc(str: String): String = URLEncoder.encode(str, StandardCharsets.UTF_8)
  fun toUriQueryString(): String {
    return "response_type=${enc(response_type)}&response_mode=${enc(response_mode)}&client_id=${enc(client_id)}&redirect_uri=${enc(redirect_uri)}" +
           "&scope=${enc(scope)}&nonce=${enc(nonce)}&registration=${enc(Klaxon().toJsonString(registration))}" +
           "&exp=$expiration&iat=$issuedAt&claims=${enc(Klaxon().toJsonString(claims))}"
  }

  companion object {
    fun fromHttpContext(ctx: Context): SIOPv2Request {
      val requiredParams = setOf("client_id", "redirect_uri", "nonce", "registration", "exp", "iat", "claims")
      if (requiredParams.any { ctx.queryParam(it).isNullOrEmpty() })
        throw IllegalArgumentException("HTTP context missing mandatory query parameters")
      return SIOPv2Request(
        ctx.queryParam("response_type") ?: "id_token",
        ctx.queryParam("client_id")!!,
        ctx.queryParam("redirect_uri")!!,
        ctx.queryParam("response_mode") ?: "fragment",
        ctx.queryParam("scope") ?: "openid",
        ctx.queryParam("nonce")!!,
        Klaxon().parse<Registration>(ctx.queryParam("registration")!!)!!,
        ctx.queryParam("exp")!!.toLong(),
        ctx.queryParam("iat")!!.toLong(),
        Klaxon().parse<Claims>(ctx.queryParam("claims")!!)!!
      )
    }
  }
}

data class Registration(
  val subject_identifier_types_supported: List<String> = listOf("did"),
  val did_methods_supported: List<String> = listOf("did:ebsi:"),
  val vp_formats: VPFormats = VPFormats(),
  val client_name: String? = null,
  val client_purpose: String? = null,
  val tos_uri: String? = null,
  val logo_uri: String? = null
)

data class VPFormats(
  val jwt_vp: JwtVPFormat? = JwtVPFormat(),
  val ldp_vp: LdpVpFormat? = LdpVpFormat()
)

data class JwtVPFormat (
  val alg: Set<String> = setOf("EdDSA", "ES256K")
)

data class  LdpVpFormat(
  val proof_type: Set<String> = setOf("Ed25519Signature2018")
)

data class VpSchema (
  val uri: String
)

data class InputDescriptor (
  val id: String,
  val schema: VpSchema
    )

data class PresentationDefinition (
  val id: String,
  val input_descriptors: List<InputDescriptor>
    )

data class VpTokenClaim (
  val presentation_definition: PresentationDefinition
    )

data class Claims (
  val vp_token: VpTokenClaim? = null
    )
