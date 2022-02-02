package id.walt.cli

import com.beust.klaxon.JsonObject
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.nimbusds.jwt.JWT
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.oauth2.sdk.token.AccessToken
import com.nimbusds.oauth2.sdk.token.AccessTokenType
import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import id.walt.common.prettyPrint
import id.walt.custodian.Custodian
import id.walt.model.oidc.CredentialClaim
import id.walt.model.oidc.OIDCIssuer
import id.walt.services.did.DidService
import id.walt.services.jwt.JwtService
import id.walt.vclib.model.Proof
import id.walt.vclib.templates.VcTemplateManager
import net.minidev.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Collectors


class OidcCommand : CliktCommand(name = "oidc", help = """OIDC for verifiable presentation and credential issuance
  
  OIDC commands, related to credential presentation and credential issuance through OIDC SIOPv2 specifications
""") {
  override fun run() {
  }
}

class OidcIssuanceCommand: CliktCommand(name = "issue", help = "Credential issuance") {
  override fun run() {
  }
}

class OidcIssuanceAuthCommand: CliktCommand(name = "auth", help = "OIDC issuance authorization step") {
  val mode: String by option("-m", "--mode", help = "Authorization mode [pushed|ebsi|direct]").default("pushed")
  val issuer: String by option("-i", "--issuer", help = "Issuer base URL").required()
  val client_id: String? by option("--client-id", help = "Client ID for authorization at the issuer API")
  val client_secret: String? by option("--client-secret", help = "Client Secret for authorization at the issuer API")
  val redirect_uri: String by option("--redirect-uri", help = "Redirect URI to send with the authorization request").default("http://blank")
  val schema_ids: List<String> by option("--schema-id", help = "Schema ID of credential to be issued").multiple(default = listOf(VcTemplateManager.loadTemplate("VerifiableId").credentialSchema!!.id))

  override fun run() {
    val issuer = OIDCIssuer(issuer, issuer, client_id = client_id, client_secret = client_secret)
    issuer.init()
    val credentialClaims = schema_ids.map { CredentialClaim(type = it, manifest_id = null) }
    println(
      when(mode) {
        "ebsi" -> {
          issuer.executeGetAuthorizationRequest(URI.create(redirect_uri), credentialClaims)
        }
        "direct" -> {
          println("Point your browser to this address:")
          issuer.getUserAgentAuthorizationURL(URI.create(redirect_uri), credentialClaims)
        }
        else -> {
          println("Point your browser to this address, then paste the redirection url to the command 'oidc issue token' with the argument '--from-redirect-url':")
          issuer.executePushedAuthorizationRequest(URI.create(redirect_uri), credentialClaims)
        }
      } ?: "<Error: No result>")
  }
}

class OidcIssuanceTokenCommand: CliktCommand(name = "token", help = "Get access token using authorization code from auth command") {

  val mode: String by option("-m", "--mode", help = "Request body mode [oidc|ebsi]").default("oidc")
  val issuer: String by option("-i", "--issuer", help = "Issuer base URL").required()
  val code: String? by option("-c", "--code", help = "Code retrieved through previously executed auth command. Alternatively can be read from redirect-url")
  val fromRedirectUrl: String? by option("--from-redirect-url", help = "Paste browser redirection url after completing auth in browser")
  val client_id: String? by option("--client-id", help = "Client ID for authorization at the issuer API")
  val client_secret: String? by option("--client-secret", help = "Client Secret for authorization at the issuer API")

  override fun run() {
    val issuer = OIDCIssuer(issuer, issuer, client_id = client_id, client_secret = client_secret)
    issuer.init()

    val authCode = code ?: fromRedirectUrl?.let {
      Pattern.compile("&")
        .split(URI.create(fromRedirectUrl).query)
        .map { s -> s.split(Pattern.compile("="), 2) }
        .map { o -> Pair(o[0].let { URLDecoder.decode(it, StandardCharsets.UTF_8) }, o[1].let { URLDecoder.decode(it, StandardCharsets.UTF_8) }) }
        .toMap()["code"]
    }
    if(authCode == null) {
      println("Error: Code not specified")
    } else {
      val tokenResponse = issuer.getAccessToken(authCode, mode)
      println(tokenResponse.toJSONObject().prettyPrint())
    }
  }
}

class OidcIssuanceCredentialCommand: CliktCommand(name = "credential", help = "Get credential using access token from token command") {

  val mode: String by option("-m", "--mode", help = "Request body mode [oidc|ebsi]").default("oidc")
  val issuer: String by option("-i", "--issuer", help = "Issuer base URL").required()
  val token: String by option("-t", "--token", help = "Access token retrieved through previously executed token command.").required()
  val nonce: String by option("-n", "--nonce", help = "Nonce retrieved through previously executed token command, for proving did possession.").required()
  val did: String by option("-d", "--did", help = "Subject DID to issue credential for").required()
  val schemaId: String by option("-s", "--schema-id", help = "Schema ID of credential to be issued. Must correspond to one schema id specified in previously called auth command").default(VcTemplateManager.loadTemplate("VerifiableId").credentialSchema!!.id)
  val token_type: String by option("--token-type", help = "Access token type, as returned by previously executed token command, default: Bearer").default("Bearer")
  val client_id: String? by option("--client-id", help = "Client ID for authorization at the issuer API")
  val client_secret: String? by option("--client-secret", help = "Client Secret for authorization at the issuer API")
  val save: Boolean by option("--save", help = "Store credential in custodial credential store, default: false").flag()

  override fun run() {
    val issuer = OIDCIssuer(issuer, issuer, client_id = client_id, client_secret = client_secret)
    issuer.init()

    val didObj = DidService.load(did)
    val proof = Proof(
      type = didObj.verificationMethod!!.first().type,
      creator =  did,
      verificationMethod = didObj.verificationMethod!!.first().id,
      jws = JwtService.getService().sign(did, JWTClaimsSet.Builder().issuer(did).subject(did).claim("c_nonce", nonce).build().toString()),
      nonce = nonce
    )
    val c = issuer.getCredential(BearerAccessToken(token), did, schemaId, proof, mode)
    if(c == null)
      println("Error: no credential received")
    else {
      println(c.prettyPrint())

      if(save) {
        c.id = c.id ?: UUID.randomUUID().toString()
        Custodian.getService().storeCredential(c.id!! , c)
        println()
        println("Stored as ${c.id}")
      }
    }
  }
}
