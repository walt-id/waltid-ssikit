package id.walt.services.oidc

import com.nimbusds.oauth2.sdk.AuthorizationRequest
import com.nimbusds.oauth2.sdk.http.HTTPRequest
import id.walt.model.dif.DescriptorMapping
import id.walt.model.dif.PresentationSubmission
import id.walt.model.oidc.*
import id.walt.vclib.credentials.VerifiablePresentation
import io.javalin.http.Context
import net.minidev.json.JSONObject
import net.minidev.json.parser.JSONParser
import java.net.URI

class OIDC4VPService (val verifier: OIDCProvider) {

  val authenticationRequestEndpoint: URI
    get() = URI.create("${verifier.url}/authentication-requests")

  private fun findVPClaims(authRequest: AuthorizationRequest): SIOPClaims {
    val SIOPClaims =
      (authRequest.requestObject?.jwtClaimsSet?.claims?.get("claims")?.toString()
      ?: authRequest.customParameters["claims"]?.firstOrNull())
      ?.let { JSONParser(-1).parse(it) as JSONObject }
      ?.let { when(it.containsKey("vp_token")) {
       true -> it.toJSONString()
       else -> it.get("id_token")?.toString() // EBSI WCT: vp_token is wrongly (?) contained inside id_token object
      }}
      ?.let { klaxon.parse<SIOPClaims>(it) } ?: SIOPClaims()
    return SIOPClaims
  }

  private fun authRequest2SIOPv2Request(authReq: AuthorizationRequest): SIOPv2Request {
    return SIOPv2Request(
      redirect_uri = (authReq.requestObject?.jwtClaimsSet?.claims?.get("redirect_uri") ?: authReq.redirectionURI)?.toString() ?: "",
      response_mode = (authReq.requestObject?.jwtClaimsSet?.claims?.get("response_mode") ?: authReq.responseMode).toString() ?: "fragment",
      nonce = (authReq.requestObject?.jwtClaimsSet?.claims?.get("nonce") ?: authReq.customParameters["nonce"]?.firstOrNull())?.toString() ?: "",
      claims = findVPClaims(authReq),
      state = (authReq.requestObject?.jwtClaimsSet?.claims?.get("state") ?: authReq.state)?.toString() ?: "",
    )
  }

  fun parseSIOPv2RequestUri(uri: URI): SIOPv2Request? {
    val authReq = AuthorizationRequest.parse(uri)
    return authRequest2SIOPv2Request(authReq)
  }

  fun parseSIOPv2RequestUriFromHttpCtx(ctx: Context): SIOPv2Request? {
    val authReq = AuthorizationRequest.parse(ctx.queryString())
    return authRequest2SIOPv2Request(authReq)
  }

  fun fetchSIOPv2Request(): SIOPv2Request? {
    val resp = HTTPRequest(HTTPRequest.Method.GET, authenticationRequestEndpoint).send()
    if(resp.indicatesSuccess()) {
      val authReq = AuthorizationRequest.parse(resp.content)
      return authRequest2SIOPv2Request(authReq)
    }
    return null
  }

  fun getSIOPResponseFor(req: SIOPv2Request, subjectDid: String, vps: List<VerifiablePresentation>): SIOPv2Response {
    return SIOPv2Response(
      id_token = IDToken(
        subject = subjectDid,
        client_id = req.redirect_uri,
        nonce = req.nonce ?: "",
        vpTokenRef = VpTokenRef(
          presentation_submission = PresentationSubmission(
            descriptor_map = vps.map { DescriptorMapping.fromVP(it) }
          )
        )
      ),
      vp_token = vps
    )
  }

  fun postSIOPResponse(req: SIOPv2Request, resp: SIOPv2Response, mode: String): String {
    val result = HTTPRequest(HTTPRequest.Method.POST, URI.create(req.redirect_uri)).apply {
      if(mode == "json") {
        setHeader("Content-Type", "application/json")
        query = resp.toLegacyJson() // EBSI WCT expects json body with legacy format
      } else {
        query = resp.toFormBody()
      }
      followRedirects = false
    }.send()
    if(result.statusCode == 302)
      return result.location.toString()
    else
      return result.content
  }
}
