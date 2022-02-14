package id.walt.services.oidc

import com.nimbusds.oauth2.sdk.AuthorizationRequest
import id.walt.model.oidc.VCClaims
import id.walt.model.oidc.klaxon
import net.minidev.json.JSONObject
import net.minidev.json.parser.JSONParser

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
}
