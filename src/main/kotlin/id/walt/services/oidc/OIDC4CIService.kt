package id.walt.services.oidc

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.oauth2.sdk.*
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic
import com.nimbusds.oauth2.sdk.auth.Secret
import com.nimbusds.oauth2.sdk.http.HTTPRequest
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.id.Issuer
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier
import com.nimbusds.oauth2.sdk.token.AccessToken
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils
import com.nimbusds.openid.connect.sdk.*
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata
import id.walt.crypto.LdSignatureType
import id.walt.model.oidc.*
import id.walt.services.did.DidService
import id.walt.services.jwt.JwtService
import id.walt.vclib.model.AbstractVerifiableCredential
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.registry.VcTypeRegistry
import mu.KotlinLogging
import java.io.StringReader
import java.net.URI
import java.util.*

object OIDC4CIService {
  private val log = KotlinLogging.logger {}
  fun getMetadataEndpoint(issuer: OIDCProvider): URI = URI.create("${issuer.url.trimEnd('/')}/.well-known/openid-configuration")
  fun getMetadata(issuer: OIDCProvider): OIDCProviderMetadata? {
    val resp = HTTPRequest(HTTPRequest.Method.GET, getMetadataEndpoint(issuer)).send()
    return if (resp.indicatesSuccess()) {
      val jsonObj = JSONObjectUtils.parse(resp.content)
      if(!jsonObj.containsKey("subject_types_supported")) {
        // WORKAROUND to fix parsing spruce/NGI metadata document from: https://ngi-oidc4vci-test.spruceid.xyz/.well-known/openid-configuration
        jsonObj["subject_types_supported"] = listOf("public")
      }
      OIDCProviderMetadata.parse(jsonObj)
    }
    else {
      log.error { "Error loading issuer provider metadata" }
      null
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
    return (issuer.oidc_provider_metadata.customParameters["credentials_supported"]?.let {
      val jsonObj = klaxon.parseJsonObject(StringReader(it.toString()))
      jsonObj.keys.associateBy({it}) { jsonObj.obj(it)?.toJsonString()?.let { klaxon.parse<CredentialMetadata>(it) } }
    }?.filterValues { v -> v != null } as Map<String, CredentialMetadata>?) ?: mapOf()
  }

  private fun createIssuanceAuthRequest(
    issuer: OIDCProviderWithMetadata,
    redirectUri: URI,
    credentialDetails: List<CredentialAuthorizationDetails>,
    pushedAuthorization: Boolean,
    nonce: String? = null,
    state: String? = null,
    // optional: wallet's OIDC issuer url, used for dynamic credential request (OIDC4VP)
    wallet_issuer: String? = null,
    // optional: user hint to be shown vy wallet, used for dynamic credential request (OIDC4VP)
    user_hint: String? = null,
    // optional: state identifying issuance process started by issuance initiation request
    op_state: String? = null
  ): AuthenticationRequest {
    val builder = AuthenticationRequest.Builder(
      ResponseType.CODE,
      Scope(OIDCScopeValue.OPENID),
      ClientID(issuer.client_id ?: redirectUri.toString()),
      redirectUri
    )
      .state(state?.let { State(it) } ?: State())
      .nonce(nonce?.let { Nonce(it) } ?: Nonce())
      .customParameter("authorization_details", klaxon.toJsonString(credentialDetails))
      .endpointURI(if (pushedAuthorization) issuer.oidc_provider_metadata.pushedAuthorizationRequestEndpointURI else issuer.oidc_provider_metadata.authorizationEndpointURI)

    wallet_issuer?.let { builder.customParameter("wallet_issuer", it) }
    user_hint?.let { builder.customParameter("user_hint", it) }
    op_state?.let { builder.customParameter("op_state", it) }

    return builder.build()
  }

  fun executePushedAuthorizationRequest(
    issuer: OIDCProviderWithMetadata,
    redirectUri: URI,
    credentialDetails: List<CredentialAuthorizationDetails>,
    nonce: String? = null,
    state: String? = null,
    // optional: wallet's OIDC issuer url, used for dynamic credential request (OIDC4VP)
    wallet_issuer: String? = null,
    // optional: user hint to be shown vy wallet, used for dynamic credential request (OIDC4VP)
    user_hint: String? = null,
    // optional: state identifying issuance process started by issuance initiation request
    op_state: String? = null
  ): URI? {
    val response = createIssuanceAuthRequest(
      issuer,
      redirectUri,
      credentialDetails,
      pushedAuthorization = true,
      nonce,
      state, wallet_issuer, user_hint, op_state
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
    credentialDetails: List<CredentialAuthorizationDetails>,
    nonce: String? = null,
    state: String? = null,
    // optional: wallet's OIDC issuer url, used for dynamic credential request (OIDC4VP)
    wallet_issuer: String? = null,
    // optional: user hint to be shown vy wallet, used for dynamic credential request (OIDC4VP)
    user_hint: String? = null,
    // optional: state identifying issuance process started by issuance initiation request
    op_state: String? = null
  ): URI? {
    val response =
      createIssuanceAuthRequest(issuer, redirectUri, credentialDetails, pushedAuthorization = false, nonce, state, wallet_issuer, user_hint, op_state)
        .toHTTPRequest(HTTPRequest.Method.GET).apply {
          if (issuer.client_id != null && issuer.client_secret != null) {
            authorization = ClientSecretBasic(ClientID(issuer.client_id), Secret(issuer.client_secret)).toHTTPAuthorizationHeader()
          }
        }.also {
          log.info("Sending auth GET request to {}\n {}", it.uri, it.query)
        }.send()
    if (response.indicatesSuccess()) {
      return URI.create("$redirectUri?code=${response.contentAsJSONObject["code"]}&state=${response.contentAsJSONObject["state"]}")
    } else {
      log.error("Got error response from auth endpoint: {}: {}", response.statusCode, response.content)
    }
    return null
  }

  fun getUserAgentAuthorizationURL(
    issuer: OIDCProviderWithMetadata,
    redirectUri: URI,
    credentialDetails: List<CredentialAuthorizationDetails>,
    nonce: String? = null,
    state: String? = null,
    // optional: wallet's OIDC issuer url, used for dynamic credential request (OIDC4VP)
    wallet_issuer: String? = null,
    // optional: user hint to be shown vy wallet, used for dynamic credential request (OIDC4VP)
    user_hint: String? = null,
    // optional: state identifying issuance process started by issuance initiation request
    op_state: String? = null
  ): URI? {
    val req = createIssuanceAuthRequest(issuer, redirectUri, credentialDetails, pushedAuthorization = false, nonce, state, wallet_issuer, user_hint, op_state)
    return URI.create("${issuer.oidc_provider_metadata.authorizationEndpointURI}?${req.toQueryString()}")
  }

  fun getAccessToken(issuer: OIDCProviderWithMetadata, code: String, redirect_uri: String, isPreAuthorized: Boolean = false, userPin: String? = null, codeVerifier: CodeVerifier? = null): OIDCTokenResponse {
    val codeGrant = if(isPreAuthorized) {
      PreAuthorizedCodeGrant(AuthorizationCode(code), URI.create(redirect_uri), userPin, codeVerifier)
    } else {
      AuthorizationCodeGrant(AuthorizationCode(code), URI.create(redirect_uri), codeVerifier)
    }
    val clientAuth = ClientSecretBasic(issuer.client_id?. let { ClientID(it) } ?: ClientID(), issuer.client_secret?.let { Secret(it) } ?: Secret())
    val resp = TokenRequest(issuer.oidc_provider_metadata.tokenEndpointURI, clientAuth, codeGrant).toHTTPRequest().also {
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
    type: String,
    jwtProof: JwtProof,
    format: String? = null
  ): VerifiableCredential? {
    val resp = HTTPRequest(
      HTTPRequest.Method.POST,
      URI.create(issuer.oidc_provider_metadata.customParameters["credential_endpoint"].toString())
    ).apply {
      authorization = accessToken.toAuthorizationHeader()
      setHeader("Content-Type", "application/json")
      query = klaxon.toJsonString(CredentialRequest(type, format, jwtProof))
    }.also {
      log.info("Sending credential request to {}\n {}", it.uri, it.query)
    }.send()
    if (resp.indicatesSuccess()) {
      log.info("Credential received: {}", resp.content)
      val credResp = klaxon.parse<CredentialResponse>(resp.content)
      return credResp?.credential
    } else {
      log.error("Got error response from credential endpoint: {}: {}", resp.statusCode, resp.content)
    }
    return null
  }

  fun generateDidProof(issuer: OIDCProvider, did: String, nonce: String): JwtProof {
    val didObj = DidService.load(did)
    val vm = (didObj.authentication ?: didObj.assertionMethod ?: didObj.verificationMethod)?.firstOrNull()?.id ?: did
    return JwtProof(
      jwt = JwtService.getService().sign(vm, JWTClaimsSet.Builder().issuer(issuer.client_id).audience(issuer.url).issueTime(Date()).claim("nonce", nonce).build().toString()),
    )
  }

  fun getCredentialAuthorizationDetails(request: AuthorizationRequest): List<CredentialAuthorizationDetails> {
    return request.customParameters["authorization_details"]?.flatMap {
      klaxon.parseArray<AuthorizationDetails>(it) ?: listOf()
    }?.filterIsInstance<CredentialAuthorizationDetails>()?.toList() ?: listOf()
  }
}
