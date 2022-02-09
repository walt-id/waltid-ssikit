package id.walt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import id.walt.common.prettyPrint
import id.walt.custodian.Custodian
import id.walt.model.dif.InputDescriptor
import id.walt.model.dif.PresentationDefinition
import id.walt.model.dif.VpSchema
import id.walt.model.oidc.*
import id.walt.services.did.DidService
import id.walt.services.jwt.JwtService
import id.walt.services.oidc.CompatibilityMode
import id.walt.services.oidc.OIDC4CIService
import id.walt.services.oidc.OIDC4VPService
import id.walt.vclib.credentials.VerifiablePresentation
import id.walt.vclib.model.Proof
import id.walt.vclib.model.toCredential
import id.walt.vclib.templates.VcTemplateManager
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.regex.Pattern


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

class OidcVerificationCommand: CliktCommand(name = "verify", help = "Credential verification") {
  override fun run() {
  }
}

class OidcIssuanceAuthCommand: CliktCommand(name = "auth", help = "OIDC issuance authorization step") {
  val mode: String by option("-m", "--mode", help = "Authorization mode [push|get|redirect]").default("push")
  val issuer_url: String by option("-i", "--issuer", help = "Issuer base URL").required()
  val nonce: String? by option("-n", "--nonce", help = "Nonce for auth request, to sign with id_token returned by tokens request.")
  val client_id: String? by option("--client-id", help = "Client ID for authorization at the issuer API")
  val client_secret: String? by option("--client-secret", help = "Client Secret for authorization at the issuer API")
  val redirect_uri: String by option("-r", "--redirect-uri", help = "Redirect URI to send with the authorization request").default("http://blank")
  val schema_ids: List<String> by option("-s", "--schema-id", help = "Schema ID of credential to be issued").multiple(default = listOf(VcTemplateManager.loadTemplate("VerifiableId").credentialSchema!!.id))

  override fun run() {
    val issuer = OIDC4CIService(OIDCProvider(issuer_url, issuer_url, client_id = client_id, client_secret = client_secret))
    val credentialClaims = schema_ids.map { CredentialClaim(type = it, manifest_id = null) }
    when(mode) {
        "get" -> {
          val redirectUri = issuer.executeGetAuthorizationRequest(URI.create(redirect_uri), credentialClaims, nonce = nonce)
          println()
          println("Client redirect URI:")
          println(redirectUri)
          println()
          println("Now get the token using:")
          println("ssikit oidc issue token -i $issuer_url -m ebsi_wct -r \"$redirectUri\"")
        }
        "redirect" -> {
          val userAgentUri = issuer.getUserAgentAuthorizationURL(URI.create(redirect_uri), credentialClaims, nonce = nonce)
          println()
          println("Point your browser to this address and authorize with the issuer:")
          println(userAgentUri)
          println()
          println("Then paste redirection url from browser to this command to retrieve the access token:")
          println("ssikit oidc issue token -i $issuer_url -r <url from browser>")

        }
        else -> {
          val userAgentUri = issuer.executePushedAuthorizationRequest(URI.create(redirect_uri), credentialClaims, nonce = nonce)
          println()
          println("Point your browser to this address and authorize with the issuer:")
          println(userAgentUri)
          println()
          println("Then paste redirection url from browser to this command to retrieve the access token:")
          println("ssikit oidc issue token -i $issuer_url -r <url from browser>")
        }
      }
  }
}

class OidcIssuanceTokenCommand: CliktCommand(name = "token", help = "Get access token using authorization code from auth command") {

  val mode: CompatibilityMode by option("-m", "--mode", help = "Request body mode [oidc|ebsi_wct]").enum<CompatibilityMode>().default(CompatibilityMode.OIDC)
  val issuer_url: String by option("-i", "--issuer", help = "Issuer base URL").required()
  val code: String? by option("-c", "--code", help = "Code retrieved through previously executed auth command. Alternatively can be read from redirect-url if specified")
  val redirect_uri: String by option("-r", "--redirect-uri", help = "Redirect URI, same as in 'oidc issue auth' command, can contain ?code parameter, to read code from").default("http://blank")
  val client_id: String? by option("--client-id", help = "Client ID for authorization at the issuer API")
  val client_secret: String? by option("--client-secret", help = "Client Secret for authorization at the issuer API")

  override fun run() {
    val issuer = OIDC4CIService(OIDCProvider(issuer_url, issuer_url, client_id = client_id, client_secret = client_secret))
    val redirectUri = URI.create(redirect_uri)
    val authCode = code ?: redirect_uri?.let {
      Pattern.compile("&")
        .split(redirectUri.query)
        .map { s -> s.split(Pattern.compile("="), 2) }
        .map { o -> Pair(o[0].let { URLDecoder.decode(it, StandardCharsets.UTF_8) }, o[1].let { URLDecoder.decode(it, StandardCharsets.UTF_8) }) }
        .toMap()["code"]
    }
    if(authCode == null) {
      println("Error: Code not specified")
    } else {
      val tokenResponse = issuer.getAccessToken(authCode, redirect_uri.substringBeforeLast("?"), mode)
      println("Access token response:")
      val jsonObj = tokenResponse.toJSONObject()
      println(jsonObj.prettyPrint())
      println()
      println("Now get the credential using:")
      println("ssikit oidc issue credential -i $issuer_url -m $mode -t ${jsonObj.get("access_token") ?: "<token>"} ${jsonObj.get("c_nonce")?.let { "-n $it" } ?: ""} -d <subject did> -t <credential schema id>")
    }
  }
}

class OidcIssuanceCredentialCommand: CliktCommand(name = "credential", help = "Get credential using access token from token command") {

  val mode: CompatibilityMode by option("-m", "--mode", help = "Request body mode [oidc|ebsi_wct]").enum<CompatibilityMode>().default(CompatibilityMode.OIDC)
  val issuer_url: String by option("-i", "--issuer", help = "Issuer base URL").required()
  val token: String by option("-t", "--token", help = "Access token retrieved through previously executed token command.").required()
  val nonce: String? by option("-n", "--nonce", help = "Nonce retrieved through previously executed token command, for proving did possession.")
  val did: String by option("-d", "--did", help = "Subject DID to issue credential for").required()
  val schemaId: String by option("-s", "--schema-id", help = "Schema ID of credential to be issued. Must correspond to one schema id specified in previously called auth command").default(VcTemplateManager.loadTemplate("VerifiableId").credentialSchema!!.id)
  val format: String? by option("-f", "--format", help = "Preferred credential format, default: form: ldp_vc or json: jwt_vc depending on mode")
  val token_type: String by option("--token-type", help = "Access token type, as returned by previously executed token command, default: Bearer").default("Bearer")
  val client_id: String? by option("--client-id", help = "Client ID for authorization at the issuer API")
  val client_secret: String? by option("--client-secret", help = "Client Secret for authorization at the issuer API")
  val save: Boolean by option("--save", help = "Store credential in custodial credential store, default: false").flag()

  override fun run() {
    val issuer = OIDC4CIService(OIDCProvider(issuer_url, issuer_url, client_id = client_id, client_secret = client_secret))

    val didObj = DidService.load(did)
    val proof = Proof(
      type = didObj.verificationMethod!!.first().type,
      creator =  did,
      verificationMethod = didObj.verificationMethod!!.first().id,
      jws = JwtService.getService().sign(did, JWTClaimsSet.Builder().issuer(did).subject(did).claim("c_nonce", nonce).build().toString()),
      nonce = nonce
    )
    val c = issuer.getCredential(BearerAccessToken(token), did, schemaId, proof, format, mode)
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

class OidcVerificationGetUrlCommand: CliktCommand(name = "get-url", help = "Get authentication request url, to initiate verification session") {

  val verifier_url: String by option("-v", "--verifier", help = "Verifier base URL").required()
  val client_url: String by option("-c", "--client-url", help = "Base URL of client, e.g. Wallet, default: openid://").default("openid://")

  override fun run() {
    val verifier = OIDC4VPService(OIDCProvider(verifier_url, verifier_url))
    val req = verifier.fetchSIOPv2Request()
    if(req == null) {
      println("<Error fetching redirection url>")
    } else {
      println("$client_url?${req.toUriQueryString()}")
    }
  }
}

class OidcVerificationGenUrlCommand: CliktCommand(name = "gen-url", help = "Get authentication request url, to initiate verification session") {

  val verifier_url: String by option("-v", "--verifier", help = "Verifier base URL").required()
  val verifier_path: String by option("-p", "--path", help = "API path relative verifier url, to redirect the response to").required()
  val client_url: String by option("-c", "--client-url", help = "Base URL of client, e.g. Wallet, default: openid://").default("openid://")
  val response_mode: String by option("-r", "--response-mode", help = "Response mode, default: fragment").default("fragment")
  val nonce: String? by option("-n", "--nonce", help = "Nonce, default: auto-generated")
  val schemaIds: List<String> by option("-s", "--schema-id", help = "Schema IDs to request, supports multiple").multiple(listOf())
  val state: String? by option("--state", help = "State to be passed through")

  override fun run() {
    val req = SIOPv2Request(
      redirect_uri = "${verifier_url.trimEnd('/')}/${verifier_path.trimStart('/')}",
      response_mode = response_mode,
      nonce = nonce ?: UUID.randomUUID().toString(),
      claims = SIOPClaims(vp_token = VpTokenClaim(PresentationDefinition(schemaIds.map { InputDescriptor(VpSchema(it)) }))),
      state = state
    )
    println("${client_url}?${req.toUriQueryString()}")
  }
}

class OidcVerificationRespondCommand: CliktCommand(name = "present", help = "Create presentation response, and post to verifier") {

  val authUrl: String? by option("-u", "--url", help = "Authentication request URL from verifier, or get-url / gen-url subcommands")
  val did: String by option("-d", "--did", help = "Subject DID of presented credential(s)").required()
  val credentialIds: List<String> by option("-c", "--credential-id", help = "One or multiple credential IDs to be presented").multiple(listOf())
  val mode: CompatibilityMode by option("-m", "--mode", help = "Request body mode [oidc|ebsi_wct]").enum<CompatibilityMode>().default(CompatibilityMode.OIDC)

  override fun run() {
    val verifier = OIDC4VPService(OIDCProvider("", ""))
    val req = verifier.parseSIOPv2RequestUri(URI.create(authUrl))
    val resp = verifier.getSIOPResponseFor(req!!, did, listOf(Custodian.getService().createPresentation(credentialIds.map { Custodian.getService().getCredential(it)!!.encode() }, did, challenge = req.nonce, expirationDate = null).toCredential() as VerifiablePresentation))
    println("Presentation response:")
    println(resp.toFormParams().prettyPrint())

    println()
    val result = verifier.postSIOPResponse(req, resp, mode)
    println()
    println("Response:")
    println(result)
  }
}
