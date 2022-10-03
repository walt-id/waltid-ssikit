package id.walt.services.oidc

import com.beust.klaxon.Klaxon
import com.nimbusds.jose.JWSAlgorithm
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
import id.walt.crypto.LdSignatureType
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

object OIDC4CIService {
  private val log = KotlinLogging.logger {}
  fun getMetadataEndpoint(issuer: OIDCProvider) = URI.create("${issuer.url.trimEnd('/')}/.well-known/openid-configuration")
  fun getMetadata(issuer: OIDCProvider): OIDCProviderMetadata? {
    val resp = HTTPRequest(HTTPRequest.Method.GET, getMetadataEndpoint(issuer)).send()
    if (resp.indicatesSuccess())
      return OIDCProviderMetadata.parse(resp.content)
    else {
      log.error { "Error loading issuer provider metadata" }
      return null
    }
  }

  fun getWithProviderMetadata(issuer: OIDCProvider): OIDCProviderWithMetadata {
    return OIDCProviderWithMetadata(
      issuer.id, issuer.url, issuer.description, issuer.client_id, issuer.client_secret,
      getMetadata(issuer) ?: OIDCProviderMetadata(
        Issuer(issuer.url),
        listOf(SubjectType.PAIRWISE, SubjectType.PUBLIC),
        URI.create("http://blank")
      )
      .apply {
        authorizationEndpointURI = URI("${issuer.url}/authorize")
        pushedAuthorizationRequestEndpointURI = URI("${issuer.url}/par")
        tokenEndpointURI = URI("${issuer.url}/token")
        setCustomParameter("credential_endpoint", "${issuer.url}/credential")
        setCustomParameter("nonce_endpoint", "${issuer.url}/nonce")
        setCustomParameter("credential_issuer", CredentialIssuer(listOf(
          CredentialIssuerDisplay("Issuer")
        )))
        setCustomParameter("credentials_supported", VcTypeRegistry.getTypesWithTemplate().values
          .filter {
            it.isPrimary &&
                AbstractVerifiableCredential::class.java.isAssignableFrom(it.vc.java) &&
                !it.metadata.template?.invoke()?.credentialSchema?.id.isNullOrEmpty()
          }
          .map {cred -> mapOf(
            cred.metadata.type.last() to CredentialMetadata(
              formats = mapOf(
                "ldp_vc" to CredentialFormat(
                  types = cred.metadata.type,
                  cryptographic_binding_methods_supported = listOf("did"),
                  cryptographic_suites_supported = LdSignatureType.values().map { it.name }
                ),
                "jwt_vc" to CredentialFormat(
                  types = cred.metadata.type,
                  cryptographic_binding_methods_supported = listOf("did"),
                  cryptographic_suites_supported = listOf(JWSAlgorithm.ES256K, JWSAlgorithm.EdDSA, JWSAlgorithm.RS256, JWSAlgorithm.PS256).map { it.name }
                )
              ),
              display = listOf(
                CredentialDisplay(
                  name = cred.metadata.type.last()
                )
              )
            )
          )}
        )
      }
    )
  }

  fun getIssuerInfo(issuer: OIDCProviderWithMetadata): CredentialIssuer? {
    return issuer.oidc_provider_metadata.customParameters["credential_issuer"]?.let {
      klaxon.parse(it.toString())
    }
  }

  fun getSupportedCredentials(issuer: OIDCProviderWithMetadata): Map<String, CredentialMetadata> {
    return issuer.oidc_provider_metadata.customParameters["credentials_supported"]?.let {
      klaxon.parse(it.toString())
    } ?: mapOf()
  }

  private fun createIssuanceAuthRequest(
    issuer: OIDCProviderWithMetadata,
    redirectUri: URI,
    claimedCredentials: List<CredentialClaim>,
    pushedAuthorization: Boolean,
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
      .endpointURI(if (pushedAuthorization) issuer.oidc_provider_metadata.pushedAuthorizationRequestEndpointURI else issuer.oidc_provider_metadata.authorizationEndpointURI)

    if(vp_token != null) {
      builder.customParameter("vp_token", OIDCUtils.toVpToken(vp_token))
    }
    return builder.build()
  }

  fun executePushedAuthorizationRequest(
    issuer: OIDCProviderWithMetadata,
    redirectUri: URI,
    claimedCredentials: List<CredentialClaim>,
    vp_token: List<VerifiablePresentation>? = null,
    nonce: String? = null,
    state: String? = null
  ): URI? {
    val response = createIssuanceAuthRequest(
      issuer,
      redirectUri,
      claimedCredentials,
      pushedAuthorization = true,
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
      return URI.create("${issuer.oidc_provider_metadata.authorizationEndpointURI}?client_id=${issuer.client_id ?: redirectUri}&request_uri=${
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
    issuer: OIDCProviderWithMetadata,
    redirectUri: URI,
    claimedCredentials: List<CredentialClaim>,
    vp_token: List<VerifiablePresentation>? = null,
    nonce: String? = null,
    state: String? = null
  ): URI? {
    val response =
      createIssuanceAuthRequest(issuer, redirectUri, claimedCredentials, pushedAuthorization = false, vp_token, nonce, state)
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
    issuer: OIDCProviderWithMetadata,
    redirectUri: URI,
    claimedCredentials: List<CredentialClaim>,
    vp_token: List<VerifiablePresentation>? = null,
    nonce: String? = null,
    state: String? = null
  ): URI? {
    val req = createIssuanceAuthRequest(issuer, redirectUri, claimedCredentials, pushedAuthorization = false, vp_token, nonce, state)
    return URI.create("${issuer.oidc_provider_metadata.authorizationEndpointURI}?${req.toQueryString()}")
  }

  fun getAccessToken(issuer: OIDCProviderWithMetadata, code: String, redirect_uri: String, mode: CompatibilityMode = CompatibilityMode.OIDC): OIDCTokenResponse {
    val codeGrant = AuthorizationCodeGrant(AuthorizationCode(code), URI.create(redirect_uri))
    val clientAuth = ClientSecretBasic(issuer.client_id?. let { ClientID(it) } ?: ClientID(), issuer.client_secret?.let { Secret(it) } ?: Secret())
    val resp = TokenRequest(issuer.oidc_provider_metadata.tokenEndpointURI, clientAuth, codeGrant).toHTTPRequest().apply {
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
    issuer: OIDCProviderWithMetadata,
    accessToken: AccessToken,
    did: String,
    schemaId: String,
    proof: Proof,
    format: String? = null,
    mode: CompatibilityMode = CompatibilityMode.OIDC
  ): VerifiableCredential? {
    val resp = HTTPRequest(
      HTTPRequest.Method.POST,
      URI.create(issuer.oidc_provider_metadata.customParameters["credential_endpoint"].toString())
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

  fun getNonce(issuer: OIDCProviderWithMetadata): NonceResponse? {
    val resp = HTTPRequest(HTTPRequest.Method.POST, URI.create(issuer.oidc_provider_metadata.customParameters["nonce_endpoint"].toString())).apply {
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
