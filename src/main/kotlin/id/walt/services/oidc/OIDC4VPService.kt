package id.walt.services.oidc

import com.nimbusds.oauth2.sdk.AuthorizationRequest
import com.nimbusds.oauth2.sdk.http.HTTPRequest
import id.walt.model.dif.DescriptorMapping
import id.walt.model.dif.PresentationSubmission
import id.walt.model.oidc.*
import id.walt.vclib.credentials.VerifiablePresentation
import io.javalin.http.Context
import mu.KotlinLogging
import java.net.URI

class OIDC4VPService (val verifier: OIDCProvider) {

  private val log = KotlinLogging.logger {}
  val authenticationRequestEndpoint: URI
    get() = URI.create("${verifier.url}/authentication-requests")

  private fun authRequest2SIOPv2Request(authReq: AuthorizationRequest): SIOPv2Request {
    return SIOPv2Request(
      redirect_uri = (authReq.requestObject?.jwtClaimsSet?.claims?.get("redirect_uri") ?: authReq.redirectionURI)?.toString() ?: "",
      response_mode = (authReq.requestObject?.jwtClaimsSet?.claims?.get("response_mode") ?: authReq.responseMode).toString() ?: "fragment",
      nonce = (authReq.requestObject?.jwtClaimsSet?.claims?.get("nonce") ?: authReq.customParameters["nonce"]?.firstOrNull())?.toString() ?: "",
      claims = OIDCUtils.getVCClaims(authReq),
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
    val resp = HTTPRequest(HTTPRequest.Method.GET, authenticationRequestEndpoint).also {
      log.info("Getting OIDC request params from {}\n {}", it.uri)
    }.send()
    if(resp.indicatesSuccess()) {
      val authReq = AuthorizationRequest.parse(resp.content)
      return authRequest2SIOPv2Request(authReq)
    } else {
      log.error("Got error response from auth endpoint: {}: {}", resp.statusCode, resp.content)
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
            descriptor_map = DescriptorMapping.fromVPs(vps),
            definition_id = req.claims.vp_token?.presentation_definition?.id,
            id = "1"
          )
        )
      ),
      vp_token = vps,
      state = req.state
    )
  }

  fun postSIOPResponse(req: SIOPv2Request, resp: SIOPv2Response, mode: CompatibilityMode = CompatibilityMode.OIDC): String {
    val result = HTTPRequest(HTTPRequest.Method.POST, URI.create(req.redirect_uri)).apply {
      if(mode == CompatibilityMode.EBSI_WCT) {
        setHeader("Content-Type", "application/json")
        query = resp.toEBSIWctJson() // EBSI WCT expects json body with incorrect presentation jwt format
      } else {
        query = resp.toFormBody()
      }
      followRedirects = false
    }.also {
      log.info("Sending SIOP response to {}\n {}", it.uri, it.query)
    }.send()
    if(!result.indicatesSuccess() && result.statusCode != 302) {
      log.error("Got error response from SIOP endpoint: {}: {}", result.statusCode, result.content)
    }
    if(result.statusCode == 302)
      return result.location.toString()
    else
      return result.content
  }
}
