package id.walt.services.oidc

import com.nimbusds.oauth2.sdk.AuthorizationRequest
import com.nimbusds.oauth2.sdk.ResponseMode
import com.nimbusds.oauth2.sdk.ResponseType
import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.http.HTTPRequest
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.openid.connect.sdk.OIDCScopeValue
import id.walt.model.dif.DescriptorMapping
import id.walt.model.dif.PresentationDefinition
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

  fun getSIOPResponseFor(req: AuthorizationRequest, subjectDid: String, vps: List<VerifiablePresentation>): SIOPv2Response {
    val presentationDefinition = getPresentationDefinition(req)
    return SIOPv2Response(
      vp_token = vps,
      presentation_submission = PresentationSubmission(
        descriptor_map = DescriptorMapping.fromVPs(vps),
        definition_id = presentationDefinition.id,
        id = "1"
      ),
      id_token = SelfIssuedIDToken(
        subject = subjectDid,
        client_id = req.clientID.toString(),
        nonce = req.customParameters["nonce"]?.firstOrNull(),
        expiration = null
      ),
      state = req.state.toString()
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

  companion object {
    fun createOIDCVPRequest(
      wallet_url: URI,
      redirect_uri: URI,
      nonce: String,
      response_type: ResponseType = ResponseType("vp_token"),
      response_mode: ResponseMode = ResponseMode.FRAGMENT,
      scope: String? = null,
      presentation_definition: PresentationDefinition? = null,
      presentation_definition_uri: URI? = null,
      state: String? = null
    ): AuthorizationRequest {
      if(listOf(scope, presentation_definition, presentation_definition_uri).filter { it != null && !(it is String && it.isEmpty()) }.size != 1 ) {
        throw Exception("One and only one parameter of [scope, presentation_definition, presentation_definition_url] MUST be given.")
      }
      val customParams = mutableMapOf("nonce" to listOf(nonce))
      if(scope.isNullOrEmpty()) {
        val presentationDefinitionKey = presentation_definition?.let { "presentation_definition" } ?: "presentation_definition_uri"
        val presentationDefinitionValue = presentation_definition?.let { klaxon.toJsonString(it) } ?: presentation_definition_uri!!.toString()
        customParams[presentationDefinitionKey] = listOf(presentationDefinitionValue)
      }
      return AuthorizationRequest(
        wallet_url,
        response_type,
        response_mode,
        ClientID(redirect_uri.toString()),
        redirect_uri, scope?.let { Scope(it) } ?: Scope(OIDCScopeValue.OPENID),
        state?.let { State(it) } ?: State(),
        null, null, null, false, null, null, null,
        customParams
      )
    }

    fun getPresentationDefinition(authRequest: AuthorizationRequest): PresentationDefinition {
      val scope = if(authRequest.scope.size == 1 && authRequest.scope.none { it == OIDCScopeValue.OPENID }) {
        authRequest.scope.first().toString()
      } else { null }
      val presentationDefinition = authRequest.customParameters["presentation_definition"]?.first()?.let { klaxon.parse<PresentationDefinition>(it) }
      val presentationDefinitionUri = authRequest.customParameters["presentation_definition_uri"]?.firstOrNull()
      if(listOf(scope, presentationDefinition, presentationDefinitionUri).filter { it != null && !(it is String && it.isEmpty()) }.size != 1 ) {
        throw Exception("One and only one parameter of [scope, presentation_definition, presentation_definition_url] MUST be given.")
      }
      if(!scope.isNullOrEmpty()) {
        TODO("How to find pre-definied presentation definition by scope")
      }
      if(presentationDefinition != null) {
        return presentationDefinition
      }
      val response = HTTPRequest(HTTPRequest.Method.GET, URI.create(presentationDefinitionUri!!)).send()
      if(response.indicatesSuccess()) {
        return klaxon.parse<PresentationDefinition>(response.content) ?: throw Exception("Error parsing presentation_definition_url response as PresentationDefinition object")
      }
      throw Exception("Error fetching presentation definition from presentation_definition_uri")
    }
  }
}
