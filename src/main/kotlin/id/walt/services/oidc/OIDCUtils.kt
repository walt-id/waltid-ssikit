package id.walt.services.oidc

import com.beust.klaxon.JsonBase
import com.beust.klaxon.Klaxon
import com.nimbusds.oauth2.sdk.AuthorizationRequest
import id.walt.custodian.Custodian
import id.walt.model.dif.InputDescriptor
import id.walt.model.dif.PresentationDefinition
import id.walt.model.oidc.VCClaims
import id.walt.model.oidc.klaxon
import id.walt.vclib.credentials.VerifiablePresentation
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.model.toCredential
import net.minidev.json.JSONObject
import net.minidev.json.parser.JSONParser
import java.io.StringReader
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

object OIDCUtils {
  fun getVCClaims(authRequest: AuthorizationRequest): VCClaims {
    val claims =
      (authRequest.requestObject?.jwtClaimsSet?.claims?.get("claims")?.toString()
        ?: authRequest.customParameters["claims"]?.firstOrNull())
        ?.let { JSONParser(-1).parse(it) as JSONObject }
        ?.let { when(it.containsKey("vp_token") || it.containsKey("credentials")) {
          true -> it.toJSONString()
          else -> it.get("id_token")?.toString() // EBSI WCT: vp_token is wrongly (?) contained inside id_token object
        }}
        ?.let { klaxon.parse<VCClaims>(it) } ?: VCClaims()
    return claims
  }

  fun getCodeFromRedirectUri(redirectUri: URI): String? {
    return Pattern.compile("&")
        .split(redirectUri.query)
        .map { s -> s.split(Pattern.compile("="), 2) }
        .map { o -> Pair(o[0].let { URLDecoder.decode(it, StandardCharsets.UTF_8) }, o[1].let { URLDecoder.decode(it, StandardCharsets.UTF_8) }) }
        .toMap()["code"]
  }

  fun toVpToken(vps: List<VerifiablePresentation>): String =
    when(vps.size) {
      1 -> vps[0].encode()
      else -> {
        vps.map { vp ->
          vp.jwt?.let { "\"$it\"" } ?: vp.encode()
        }.joinToString(",", "[", "]")
      }
    }

  fun fromVpToken(vp_token: String): List<VerifiablePresentation>? {
    if(vp_token.trim().startsWith('[')) {
      return Klaxon().parseJsonArray(StringReader(vp_token)).map {
        when(it) {
          is JsonBase -> it.toJsonString()
          else -> it.toString()
        }
      }.map { it.toCredential() as VerifiablePresentation }
    } else {
      return listOf(vp_token.toCredential() as VerifiablePresentation)
    }
  }

  fun matchesInputDescriptor(credential: VerifiableCredential, inputDescriptor: InputDescriptor): Boolean {
    // for now: support
    // * schema.uri from presentation exchange 1.0
    // * field constraints from presentation exchange 2.0:
    // ** paths: "$.type", "$.credentialSchema.id"
    // ** match type: pattern, const

    if(inputDescriptor.schema != null) { // PEX 1.0
      return inputDescriptor.schema.uri == credential.credentialSchema?.id
    } else { // PEX 2.0
      return inputDescriptor.constraints?.fields?.any { fld ->
          val fldVal = if(fld.path.contains("\$.type")) {
            credential.type.last()
          } else if(fld.path.contains("\$.credentialSchema.id")) {
            credential.credentialSchema?.id
          } else {
            null
          }
          return fldVal?.let {
            return if(fld.filter?.containsKey("pattern") == true) {
              Regex(fld.filter["pattern"].toString()).matches(fldVal)
            } else if(fld.filter?.containsKey("const") == true) {
              fld.filter["const"] == fldVal
            } else {
              false
            }
          } ?: false
      } ?: false
    }
  }

  /**
   * Find credentials matching input descriptors in presentation definition. optionally filter by subject DID
   * @return Map from input_descriptor id to set of matching credential ids
   */
  fun findCredentialsFor(presentationDefinition: PresentationDefinition, subject: String? = null): Map<String, Set<String>> {

    val myCredentials = Custodian.getService().listCredentials()
    return presentationDefinition.input_descriptors.map { indesc ->
      Pair(indesc.id, myCredentials.filter {
          c -> matchesInputDescriptor(c, indesc) &&
          (subject == null || subject == c.subject)
      }.map { c -> c.id!! }.toSet())
    }.toMap()
  }
}
