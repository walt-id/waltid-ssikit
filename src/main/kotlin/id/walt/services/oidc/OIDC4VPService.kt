package id.walt.services.oidc

import com.nimbusds.jose.shaded.gson.internal.LinkedTreeMap
import com.nimbusds.oauth2.sdk.AuthorizationRequest
import com.nimbusds.oauth2.sdk.ResponseMode
import com.nimbusds.oauth2.sdk.ResponseType
import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.http.HTTPRequest
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.openid.connect.sdk.Nonce
import com.nimbusds.openid.connect.sdk.OIDCResponseTypeValue
import com.nimbusds.openid.connect.sdk.OIDCScopeValue
import id.walt.model.dif.DescriptorMapping
import id.walt.model.dif.PresentationDefinition
import id.walt.model.dif.PresentationSubmission
import id.walt.model.oidc.OIDCProvider
import id.walt.model.oidc.SIOPv2Response
import id.walt.model.oidc.SelfIssuedIDToken
import id.walt.model.oidc.klaxon
import id.walt.vclib.credentials.VerifiablePresentation
import io.javalin.http.Context
import mu.KotlinLogging
import java.net.URI

object OIDC4VPService {
    private val log = KotlinLogging.logger {}
    private fun getAuthenticationRequestEndpoint(verifier: OIDCProvider) = URI.create("${verifier.url}/authentication-requests")

    fun fetchOIDC4VPRequest(verifier: OIDCProvider): AuthorizationRequest? {
        val resp = HTTPRequest(HTTPRequest.Method.GET, getAuthenticationRequestEndpoint(verifier)).also {
            log.info("Getting OIDC request params from {}\n {}", it.uri)
        }.send()
        if (resp.indicatesSuccess()) {
            return AuthorizationRequest.parse(resp.content)
        } else {
            log.error("Got error response from auth endpoint: {}: {}", resp.statusCode, resp.content)
        }
        return null
    }

    fun createOIDC4VPRequest(
        wallet_url: URI,
        redirect_uri: URI,
        nonce: Nonce,
        response_type: ResponseType = ResponseType("vp_token"),
        response_mode: ResponseMode = ResponseMode.FRAGMENT,
        scope: Scope? = null,
        presentation_definition: PresentationDefinition? = null,
        presentation_definition_uri: URI? = null,
        state: State? = null,
        customParameters: Map<String, List<String>>? = null
    ): AuthorizationRequest {

        val presentationByScope = scope?.let {
            it.size == 1 && it.none { it == OIDCScopeValue.OPENID }
        } ?: false

        if (listOf(
                presentationByScope,
                presentation_definition != null,
                presentation_definition_uri != null
            ).count { it } != 1
        ) {
            throw Exception("One and only one parameter of [single-scope, presentation_definition, presentation_definition_url] MUST be given.")
        }
        val customParams = mutableMapOf("nonce" to listOf(nonce.value))
        if (!presentationByScope) {
            val presentationDefinitionKey =
                presentation_definition?.let { "presentation_definition" } ?: "presentation_definition_uri"
            val presentationDefinitionValue =
                presentation_definition?.let { klaxon.toJsonString(it) } ?: presentation_definition_uri!!.toString()
            customParams[presentationDefinitionKey] = listOf(presentationDefinitionValue)
        }
        customParameters?.let { customParams.putAll(customParameters) }
        return AuthorizationRequest(
            wallet_url,
            response_type,
            response_mode,
            ClientID(redirect_uri.toString()),
            redirect_uri, scope?.let { Scope(it) } ?: Scope(OIDCScopeValue.OPENID),
            state ?: State(),
            null, null, null, false, null, null, null,
            customParams
        )
    }

    fun getPresentationDefinition(authRequest: AuthorizationRequest): PresentationDefinition {
        val scope = if (authRequest.scope.size == 1 && authRequest.scope.none { it == OIDCScopeValue.OPENID }) {
            authRequest.scope.first().toString()
        } else {
            null
        }
        val presentationDefinition =
            authRequest.customParameters["presentation_definition"]?.first()?.let { klaxon.parse<PresentationDefinition>(it) }
        val presentationDefinitionUri = authRequest.customParameters["presentation_definition_uri"]?.firstOrNull()
        if (listOf(
                scope,
                presentationDefinition,
                presentationDefinitionUri
            ).filter { it != null && !(it is String && it.isEmpty()) }.size != 1
        ) {
            throw Exception("One and only one parameter of [scope, presentation_definition, presentation_definition_url] MUST be given.")
        }
        if (!scope.isNullOrEmpty()) {
            TODO("How to find pre-definied presentation definition by scope")
        }
        if (presentationDefinition != null) {
            return presentationDefinition
        }
        val response = HTTPRequest(HTTPRequest.Method.GET, URI.create(presentationDefinitionUri!!)).send()
        if (response.indicatesSuccess()) {
            return klaxon.parse<PresentationDefinition>(response.content)
                ?: throw Exception("Error parsing presentation_definition_url response as PresentationDefinition object")
        }
        throw Exception("Error fetching presentation definition from presentation_definition_uri")
    }

    private fun authRequest2OIDC4VPRequest(authReq: AuthorizationRequest): AuthorizationRequest {
        return createOIDC4VPRequest(
            authReq.requestURI ?: URI.create("openid:///"),
            redirect_uri = authReq.requestObject?.jwtClaimsSet?.claims?.get("redirect_uri")?.let { URI.create(it.toString()) }
                ?: authReq.redirectionURI,
            nonce = (authReq.requestObject?.jwtClaimsSet?.claims?.get("nonce")
                ?: authReq.customParameters["nonce"]?.firstOrNull())?.toString()?.let { Nonce(it) } ?: Nonce(),
            response_type = authReq.requestObject?.jwtClaimsSet?.claims?.get("response_type")
                ?.let { ResponseType(it.toString()) } ?: authReq.responseType,
            response_mode = authReq.requestObject?.jwtClaimsSet?.claims?.get("response_mode")
                ?.let { ResponseMode(it.toString()) } ?: authReq.responseMode,
            scope = authReq.scope,
            presentation_definition = (
                    // 1
                    (authReq.requestObject?.jwtClaimsSet?.claims?.get("presentation_definition")?.toString()
                        ?: authReq.getCustomParameter("presentation_definition")?.firstOrNull()
                            )?.let {
                            klaxon.parse<PresentationDefinition>(it)
                        }
                    // 2
                        ?: OIDCUtils.getVCClaims(authReq).vp_token?.presentation_definition
                        // 3
                        ?: {
                            val idToken = authReq.requestObject.jwtClaimsSet.getJSONObjectClaim("claims")
                                .get("id_token") as LinkedTreeMap<*, *>
                            val vpToken = idToken.get("vp_token") as LinkedTreeMap<*, *>
                            val presDef = vpToken["presentation_definition"] as LinkedTreeMap<*, *>
                            val presDefJson = id.walt.model.oidc.klaxon.toJsonString(presDef)
                            val aPresDef = id.walt.model.oidc.klaxon.parse<PresentationDefinition>(presDefJson)
                            aPresDef
                        }()
                    ) as PresentationDefinition,
            presentation_definition_uri = (authReq.requestObject?.jwtClaimsSet?.claims?.get("presentation_definition_uri")
                ?: authReq.customParameters["presentation_definition_uri"]?.firstOrNull())?.toString()?.let { URI.create(it) },
            state = authReq.requestObject?.jwtClaimsSet?.claims?.get("state")?.toString()?.let { State(it) } ?: authReq.state,
            customParameters = authReq.customParameters
        )
    }

    fun parseOIDC4VPRequestUri(uri: URI): AuthorizationRequest {
        val authReq = AuthorizationRequest.parse(uri)
        return authRequest2OIDC4VPRequest(authReq)
    }

    fun parseOIDC4VPRequestUriFromHttpCtx(ctx: Context): AuthorizationRequest {
        val authReq = AuthorizationRequest.parse(ctx.queryString())
        return authRequest2OIDC4VPRequest(authReq)
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
            id_token = if (req.responseType.contains(OIDCResponseTypeValue.ID_TOKEN)) {
                SelfIssuedIDToken(
                    subject = subjectDid,
                    client_id = req.clientID.toString(),
                    nonce = req.customParameters["nonce"]?.firstOrNull(),
                    expiration = null
                ).sign()
            } else {
                null
            },
            state = req.state.toString()
        )
    }

    fun postSIOPResponse(
        req: AuthorizationRequest,
        resp: SIOPv2Response,
        mode: CompatibilityMode = CompatibilityMode.OIDC
    ): String {
        val result = HTTPRequest(HTTPRequest.Method.POST, req.redirectionURI).apply {
            if (mode == CompatibilityMode.EBSI_WCT) {
                setHeader("Content-Type", "application/json")
                query = resp.toEBSIWctJson() // EBSI WCT expects json body with incorrect presentation jwt format
            } else {
                query = resp.toFormBody()
            }
            followRedirects = false
        }.also {
            log.info("Sending SIOP response to {}\n {}", it.uri, it.query)
        }.send()
        if (!result.indicatesSuccess() && result.statusCode != 302) {
            log.error("Got error response from SIOP endpoint: {}: {}", result.statusCode, result.content)
        }
        return if (result.statusCode == 302)
            result.location.toString()
        else
            result.content
    }
}
