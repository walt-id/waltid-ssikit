package id.walt.services.oidc

import com.beust.klaxon.Klaxon
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.oauth2.sdk.*
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic
import com.nimbusds.oauth2.sdk.auth.Secret
import com.nimbusds.oauth2.sdk.http.HTTPRequest
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.id.Issuer
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.oauth2.sdk.token.AccessToken
import com.nimbusds.openid.connect.sdk.*
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata
import id.walt.model.dif.CredentialManifest
import id.walt.model.dif.OutputDescriptor
import id.walt.model.oidc.*
import id.walt.services.did.DidService
import id.walt.services.jwt.JwtService
import id.walt.vclib.credentials.VerifiablePresentation
import id.walt.vclib.model.AbstractVerifiableCredential
import id.walt.vclib.model.Proof
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.model.toCredential
import id.walt.vclib.registry.VcTypeRegistry
import mu.KotlinLogging
import net.minidev.json.JSONObject
import net.minidev.json.parser.JSONParser
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

class OIDC4CIService(
  val issuer: OIDCProvider
) {
  private val log = KotlinLogging.logger {}
  val metadataEndpoint: URI
    get() = URI.create("${issuer.url.trimEnd('/')}/.well-known/openid-configuration")

  val credentialManifests: List<CredentialManifest>
    get() = metadata?.getCustomParameter("credential_manifests")?.let { klaxon.parseArray(it.toString()) } ?: listOf()

  var metadata: OIDCProviderMetadata? = null
    get() {
      if (field == null) {
        val resp = HTTPRequest(HTTPRequest.Method.GET, metadataEndpoint).send()
        if (resp.indicatesSuccess())
          field = OIDCProviderMetadata.parse(resp.content)
        else {
          // initialize default metadata
          log.warn("Cannot get OIDC provider configuration ({}: {}), falling back to defaults", resp.statusCode, resp.content)
          field = OIDCProviderMetadata(
            Issuer(issuer.url),
            listOf(SubjectType.PAIRWISE, SubjectType.PUBLIC),
            URI.create("http://blank")
          )
            .also {
              it.authorizationEndpointURI = URI("${issuer.url}/authorize")
              it.pushedAuthorizationRequestEndpointURI = URI("${issuer.url}/par")
              it.tokenEndpointURI = URI("${issuer.url}/token")
              it.setCustomParameter("credential_endpoint", "${issuer.url}/credential")
              it.setCustomParameter("nonce_endpoint", "${issuer.url}/nonce")
              it.setCustomParameter("credential_manifests", listOf(
                CredentialManifest(
                  issuer = id.walt.model.dif.Issuer("", ""),
                  outputDescriptors = VcTypeRegistry.getTypesWithTemplate().values
                    .filter {
                      it.isPrimary &&
                          AbstractVerifiableCredential::class.java.isAssignableFrom(it.vc.java) &&
                          !it.metadata.template?.invoke()?.credentialSchema?.id.isNullOrEmpty()
                    }
                    .map {
                      OutputDescriptor(
                        it.metadata.type.last(),
                        it.metadata.template!!.invoke()!!.credentialSchema!!.id,
                        it.metadata.type.last()
                      )
                    }
                )).map { net.minidev.json.parser.JSONParser().parse(Klaxon().toJsonString(it)) }
              )
            }
        }
      }
      return field
    }

  private fun createIssuanceAuthRequest(
    endpoint: URI,
    redirectUri: URI,
    claimedCredentials: List<CredentialClaim>,
    vp_token: List<VerifiablePresentation>? = null,
    nonce: String? = null,
    state: String? = null
  ): AuthenticationRequest {
    val builder = AuthenticationRequest.Builder(
      ResponseType.CODE,
      Scope(OIDCScopeValue.OPENID),
      ClientID(issuer.client_id ?: redirectUri.toString()),
      redirectUri
    )
      .state(state?.let { State(it) } ?: State())
      .nonce(nonce?.let { Nonce(it) } ?: Nonce())
      .claims(VCClaims(credentials = claimedCredentials))
      .endpointURI(endpoint)

    if(vp_token != null) {
      builder.customParameter("vp_token", OIDCUtils.toVpToken(vp_token))
    }
    return builder.build()
  }

  fun executePushedAuthorizationRequest(
    redirectUri: URI,
    claimedCredentials: List<CredentialClaim>,
    vp_token: List<VerifiablePresentation>? = null,
    nonce: String? = null,
    state: String? = null
  ): URI? {
    val response = createIssuanceAuthRequest(
      metadata!!.pushedAuthorizationRequestEndpointURI,
      redirectUri,
      claimedCredentials,
      vp_token,
      nonce,
      state
    )
      .toHTTPRequest(HTTPRequest.Method.POST).apply {
        if (issuer.client_id != null && issuer.client_secret != null) {
          authorization = ClientSecretBasic(ClientID(issuer.client_id), Secret(issuer.client_secret)).toHTTPAuthorizationHeader()
        }
      }.also {
        log.info("Sending PAR request to {}\n {}", it.uri, it.query)
      }.send()
    if (response.indicatesSuccess()) {
      return URI.create("${metadata!!.authorizationEndpointURI}?client_id=${issuer.client_id ?: redirectUri}&request_uri=${
        PushedAuthorizationResponse.parse(
          response
        ).toSuccessResponse().requestURI
      }")
    } else {
      log.error("Got error response from PAR endpoint: {}: {}", response.statusCode, response.content)
    }
    return null
  }

  // only meant for EBSI WCT (https://api.conformance.intebsi.xyz/docs/?urls.primaryName=Conformance%20API#/Mock%20Credential%20Issuer/get-conformance-v1-issuer-mock-authorize)
  fun executeGetAuthorizationRequest(
    redirectUri: URI,
    claimedCredentials: List<CredentialClaim>,
    vp_token: List<VerifiablePresentation>? = null,
    nonce: String? = null,
    state: String? = null
  ): URI? {
    val response =
      createIssuanceAuthRequest(metadata!!.authorizationEndpointURI, redirectUri, claimedCredentials, vp_token, nonce, state)
        .toHTTPRequest(HTTPRequest.Method.GET).apply {
          if (issuer.client_id != null && issuer.client_secret != null) {
            authorization = ClientSecretBasic(ClientID(issuer.client_id), Secret(issuer.client_secret)).toHTTPAuthorizationHeader()
          }
        }.also {
          log.info("Sending auth GET request to {}\n {}", it.uri, it.query)
        }.send()
    if (response.indicatesSuccess()) {
      return URI.create("$redirectUri?code=${response.contentAsJSONObject.get("code")}&state=${response.contentAsJSONObject.get("state")}")
    } else {
      log.error("Got error response from auth endpoint: {}: {}", response.statusCode, response.content)
    }
    return null
  }

  fun getUserAgentAuthorizationURL(
    redirectUri: URI,
    claimedCredentials: List<CredentialClaim>,
    vp_token: List<VerifiablePresentation>? = null,
    nonce: String? = null,
    state: String? = null
  ): URI? {
    val req = createIssuanceAuthRequest(metadata!!.authorizationEndpointURI, redirectUri, claimedCredentials, vp_token, nonce, state)
    return URI.create("${metadata!!.authorizationEndpointURI}?${req.toQueryString()}")
  }

  fun getAccessToken(code: String, redirect_uri: String, mode: CompatibilityMode = CompatibilityMode.OIDC): OIDCTokenResponse {
    val codeGrant = AuthorizationCodeGrant(AuthorizationCode(code), URI.create(redirect_uri))
    val clientAuth = ClientSecretBasic(issuer.client_id?. let { ClientID(it) } ?: ClientID(), issuer.client_secret?.let { Secret(it) } ?: Secret())
    val resp = TokenRequest(metadata!!.tokenEndpointURI, clientAuth, codeGrant).toHTTPRequest().apply {
      if (mode == CompatibilityMode.EBSI_WCT) {
        setHeader("Content-Type", "application/json")
        query =
          "{ \"code\": \"$code\", \"grant_type\": \"${GrantType.AUTHORIZATION_CODE}\", \"redirect_uri\": \"$redirect_uri\" }"
      }
    }.also {
      log.info("Sending Token request to {}\n {}", it.uri, it.query)
    }.send()
    if(!resp.indicatesSuccess()) {
      log.error("Got error response from token endpoint: {}: {}", resp.statusCode, resp.content)
    }
    return OIDCTokenResponse.parse(resp)
  }

  fun getCredential(
    accessToken: AccessToken,
    did: String,
    schemaId: String,
    proof: Proof,
    format: String? = null,
    mode: CompatibilityMode = CompatibilityMode.OIDC
  ): VerifiableCredential? {
    val resp = HTTPRequest(
      HTTPRequest.Method.POST,
      URI.create(metadata!!.customParameters["credential_endpoint"].toString())
    ).apply {
      authorization = accessToken.toAuthorizationHeader()
      if (mode == CompatibilityMode.EBSI_WCT) {
        setHeader("Content-Type", "application/json")
        val o = JSONParser(-1).parse("""{ "did": "$did", "type": "$schemaId", "format": "${format ?: "jwt_vc" }" }""") as JSONObject
        o.put("proof", JSONParser(-1).parse(klaxon.toJsonString(proof)) as JSONObject)
        query = o.toJSONString()
      } else {
        query =
          "did=${URLEncoder.encode(did, StandardCharsets.UTF_8)}" +
          "&type=${URLEncoder.encode(schemaId, StandardCharsets.UTF_8)}" +
          "&format=${format ?: "ldp_vc"}" +
          "&proof=${URLEncoder.encode(klaxon.toJsonString(proof), StandardCharsets.UTF_8)}"
      }
    }.also {
      log.info("Sending credential request to {}\n {}", it.uri, it.query)
    }.send()
    if (resp.indicatesSuccess()) {
      log.info("Credential received: {}", resp.content)
      val credResp = klaxon.parse<CredentialResponse>(resp.content)
      return when (credResp?.format) {
        "jwt_vc" -> credResp?.credential?.toCredential()
        else -> credResp?.credential?.let { String(Base64.getUrlDecoder().decode(it)).toCredential() }
      }
    } else {
      log.error("Got error response from credential endpoint: {}: {}", resp.statusCode, resp.content)
    }
    return null
  }

  fun generateDidProof(did: String, nonce: String?): Proof {
    val didObj = DidService.load(did)
    return Proof(
      type = didObj.verificationMethod!!.first().type,
      creator =  did,
      verificationMethod = didObj.verificationMethod!!.first().id,
      jws = JwtService.getService().sign(did, JWTClaimsSet.Builder().issuer(did).subject(did).claim("c_nonce", nonce).build().toString()),
      nonce = nonce
    )
  }

  fun getNonce(): NonceResponse? {
    val resp = HTTPRequest(HTTPRequest.Method.POST, URI.create(metadata!!.customParameters["nonce_endpoint"].toString())).apply {
      if (issuer.client_id != null && issuer.client_secret != null) {
        authorization = ClientSecretBasic(ClientID(issuer.client_id), Secret(issuer.client_secret)).toHTTPAuthorizationHeader()
      }
    }.also {
      log.info("Sending nonce request to ${it.url}")
    }.send()
    if(resp.indicatesSuccess()) {
      return klaxon.parse<NonceResponse>(resp.content)
    } else {
      log.error("Got error response from nonce endpoint: {}: {}", resp.statusCode, resp.content)
      return null
    }
  }
}
