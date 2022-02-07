package id.walt.services.oidc

import com.beust.klaxon.Klaxon
import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.PushedAuthorizationResponse
import com.nimbusds.oauth2.sdk.ResponseType
import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic
import com.nimbusds.oauth2.sdk.auth.Secret
import com.nimbusds.oauth2.sdk.http.HTTPRequest
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.id.Issuer
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.oauth2.sdk.token.AccessToken
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import com.nimbusds.openid.connect.sdk.OIDCScopeValue
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse
import com.nimbusds.openid.connect.sdk.SubjectType
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata
import id.walt.model.dif.CredentialManifest
import id.walt.model.dif.OutputDescriptor
import id.walt.model.oidc.*
import id.walt.vclib.model.AbstractVerifiableCredential
import id.walt.vclib.model.Proof
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.model.toCredential
import id.walt.vclib.registry.VcTypeRegistry
import net.minidev.json.JSONObject
import net.minidev.json.parser.JSONParser
import java.net.URI
import java.util.*

class OIDC4CIService(
  val issuer: OIDCProvider
) {

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
    state: String? = null
  ): AuthenticationRequest {
    return AuthenticationRequest.Builder(
      ResponseType.CODE,
      Scope(OIDCScopeValue.OPENID),
      ClientID(redirectUri.toString()),
      redirectUri
    )
      .state(state?.let { State(it) } ?: State())
      .claims(SIOPClaims(credentials = claimedCredentials))
      .endpointURI(endpoint)
      .build()
  }

  fun executePushedAuthorizationRequest(
    redirectUri: URI,
    claimedCredentials: List<CredentialClaim>,
    state: String? = null
  ): String? {
    val response = createIssuanceAuthRequest(
      metadata!!.pushedAuthorizationRequestEndpointURI,
      redirectUri,
      claimedCredentials,
      state
    )
      .toHTTPRequest(HTTPRequest.Method.POST).apply {
        if (issuer.client_id != null && issuer.client_secret != null) {
          authorization = ClientSecretBasic(ClientID(issuer.client_id), Secret(issuer.client_secret)).toHTTPAuthorizationHeader()
        }
      }.send()
    if (response.indicatesSuccess()) {
      return "${metadata!!.authorizationEndpointURI}?client_id=$redirectUri&request_uri=${
        PushedAuthorizationResponse.parse(
          response
        ).toSuccessResponse().requestURI
      }"
    }
    return null
  }

  // only meant for EBSI WCT (https://api.conformance.intebsi.xyz/docs/?urls.primaryName=Conformance%20API#/Mock%20Credential%20Issuer/get-conformance-v1-issuer-mock-authorize)
  fun executeGetAuthorizationRequest(
    redirectUri: URI,
    claimedCredentials: List<CredentialClaim>,
    state: String? = null
  ): String? {
    val response =
      createIssuanceAuthRequest(metadata!!.authorizationEndpointURI, redirectUri, claimedCredentials, state)
        .toHTTPRequest(HTTPRequest.Method.GET).apply {
          if (issuer.client_id != null && issuer.client_secret != null) {
            authorization = ClientSecretBasic(ClientID(issuer.client_id), Secret(issuer.client_secret)).toHTTPAuthorizationHeader()
          }
        }.also {
          println(it.query)
        }.send()
    if (response.indicatesSuccess()) {
      return "$redirectUri?code=${response.contentAsJSONObject.get("code")}&state=${response.contentAsJSONObject.get("state")}"
    }
    return null
  }

  fun getUserAgentAuthorizationURL(
    redirectUri: URI,
    claimedCredentials: List<CredentialClaim>,
    state: String? = null
  ): String? {
    val req = createIssuanceAuthRequest(metadata!!.authorizationEndpointURI, redirectUri, claimedCredentials, state)
    return "${metadata!!.authorizationEndpointURI}?${req.toQueryString()}"
  }

  fun getAccessToken(code: String, mode: String = "form"): OIDCTokenResponse {
    val resp = HTTPRequest(HTTPRequest.Method.POST, metadata!!.tokenEndpointURI).apply {
      if (issuer.client_id != null && issuer.client_secret != null) {
        authorization = ClientSecretBasic(ClientID(issuer.client_id), Secret(issuer.client_secret)).toHTTPAuthorizationHeader()
      }
      if (mode == "json") {
        setHeader("Content-Type", "application/json")
        query = "{ \"code\": \"$code\", \"grant_type\": \"${GrantType.AUTHORIZATION_CODE}\" }"
      } else {
        query = "code=$code&grant_type=${GrantType.AUTHORIZATION_CODE}"
      }
    }.also {
      println(it.url)
      println(it.query)
    }.send()

    return OIDCTokenResponse.parse(resp)
  }

  fun getCredential(
    accessToken: AccessToken,
    did: String,
    schemaId: String,
    proof: Proof,
    mode: String = "form"
  ): VerifiableCredential? {
    val resp = HTTPRequest(
      HTTPRequest.Method.POST,
      URI.create(metadata!!.customParameters["credential_endpoint"].toString())
    ).apply {
      authorization = accessToken.toAuthorizationHeader()
      if (mode == "json") {
        setHeader("Content-Type", "application/json")
        val o = JSONParser(-1).parse("""{ "did": "$did", "type": "$schemaId", "format": "jwt_vc" }""") as JSONObject
        o.put("proof", JSONParser(-1).parse(klaxon.toJsonString(proof)) as JSONObject)
        query = o.toJSONString()
      } else {
        query = "did=$did&type=$schemaId"
      }
    }.also {
      println("POST body: ${it.query}")
    }.send()
    if (resp.indicatesSuccess()) {
      println("Credential received: ${resp.content}")
      val credResp = klaxon.parse<CredentialResponse>(resp.content)
      return when (credResp?.format) {
        "jwt_vc" -> credResp?.credential?.toCredential()
        else -> credResp?.credential?.let { String(Base64.getUrlDecoder().decode(it)).toCredential() }
      }
    } else {
      println("Error receiving credential: ${resp.statusCode}, ${resp.content}")
    }
    return null
  }
}
