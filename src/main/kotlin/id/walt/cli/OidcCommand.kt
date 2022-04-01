package id.walt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import id.walt.common.prettyPrint
import id.walt.custodian.Custodian
import id.walt.model.dif.InputDescriptor
import id.walt.model.dif.PresentationDefinition
import id.walt.model.dif.VCSchema
import id.walt.model.oidc.*
import id.walt.services.oidc.CompatibilityMode
import id.walt.services.oidc.OIDCUtils
import id.walt.vclib.credentials.VerifiablePresentation
import id.walt.vclib.model.toCredential
import id.walt.vclib.templates.VcTemplateManager
import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.*


class OidcCommand : CliktCommand(name = "oidc", help = """OIDC for verifiable presentation and credential issuance
  
  OIDC commands, related to credential presentation and credential issuance through OIDC SIOPv2 specifications
""") {
  override fun run() {
  }
}

class OidcIssuanceCommand: CliktCommand(name = "ci", help = "OIDC for Credential Issuance") {
  override fun run() {
  }
}

class OidcVerificationCommand: CliktCommand(name = "vp", help = "OIDC for Verifiable Presentations") {
  override fun run() {
  }
}

class OidcIssuanceInfoCommand: CliktCommand(name = "info", help = "List issuer info: supported credentials and optional VP requirements") {
  val issuer_url: String by option("-i", "--issuer", help = "Issuer base URL").required()

  override fun run() {
    val issuer = OIDCProvider(issuer_url, issuer_url)
    issuer.ciSvc.credentialManifests.forEach { m ->
      println("###")
      println("Issuer:")
      println(m.issuer.name)
      println("---")
      println("Issuable credentials:")
      m.outputDescriptors.forEach { od ->
        println("- "  + (VcTemplateManager.getTemplateList().firstOrNull { t -> VcTemplateManager.loadTemplate(t).credentialSchema?.id == od.schema } ?: "Unknown type"))
        println("Schema ID: ${od.schema}")
      }
      println("---")
      println("Required VP:")
      println(m.presentationDefinition?.input_descriptors?.map { id -> VcTemplateManager.getTemplateList().firstOrNull { t -> VcTemplateManager.loadTemplate(t).credentialSchema?.id == id.schema?.uri ?: "Unknown type" }}?.joinToString(",") ?: "<None>")
    }
  }
}

class OidcIssuanceNonceCommand: CliktCommand(name = "nonce", help = "Get nonce from issuer for required verifiable presentation") {
  val issuer_url: String by option("-i", "--issuer", help = "Issuer base URL").required()
  val client_id: String? by option("--client-id", help = "Client ID for authorization at the issuer API")
  val client_secret: String? by option("--client-secret", help = "Client Secret for authorization at the issuer API")

  override fun run() {
    val issuer = OIDCProvider(issuer_url, issuer_url, client_id = client_id, client_secret = client_secret)
    println(klaxon.toJsonString(issuer.ciSvc.getNonce()).prettyPrint())
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
  val vp: String? by option("--vp", help = "File name of VP to include in authorization request")

  override fun run() {
    val issuer = OIDCProvider(issuer_url, issuer_url, client_id = client_id, client_secret = client_secret)
    val credentialClaims = schema_ids.map { CredentialClaim(type = it, manifest_id = null) }
    val vp_token = vp?.let {
      if(File(vp).exists()) {
        File(vp).readText(StandardCharsets.UTF_8).toCredential() as VerifiablePresentation
      } else
        null
    }?.let { listOf(it) }
    when(mode) {
        "get" -> {
          val redirectUri = issuer.ciSvc.executeGetAuthorizationRequest(URI.create(redirect_uri), credentialClaims, nonce = nonce, vp_token = vp_token)
          println()
          println("Client redirect URI:")
          println(redirectUri)
          println()
          println("Now get the token using:")
          println("ssikit oidc ci token -i $issuer_url" +
              "${client_id?.let { " --client-id $client_id" } ?: ""}" +
              "${client_secret?.let { " --client-secret $client_secret" } ?: ""}" +
              " -m ebsi_wct -r \"$redirectUri\"")
        }
        "redirect" -> {
          val userAgentUri = issuer.ciSvc.getUserAgentAuthorizationURL(URI.create(redirect_uri), credentialClaims, nonce = nonce, vp_token = vp_token)
          println()
          println("Point your browser to this address and authorize with the issuer:")
          println(userAgentUri)
          println()
          println("Then paste redirection url from browser to this command to retrieve the access token:")
          println("ssikit oidc ci token -i $issuer_url" +
              "${client_id?.let { " --client-id $client_id" } ?: ""}" +
              "${client_secret?.let { " --client-secret $client_secret" } ?: ""}" +
              " -r <url from browser>")

        }
        else -> {
          val userAgentUri = issuer.ciSvc.executePushedAuthorizationRequest(URI.create(redirect_uri), credentialClaims, nonce = nonce, vp_token = vp_token)
          println()
          println("Point your browser to this address and authorize with the issuer:")
          println(userAgentUri)
          println()
          println("Then paste redirection url from browser to this command to retrieve the access token:")
          println("ssikit oidc ci token -i $issuer_url" +
              "${client_id?.let { " --client-id $client_id" } ?: ""}" +
              "${client_secret?.let { " --client-secret $client_secret" } ?: ""}" +
              " -r <url from browser>")
        }
      }
  }
}

class OidcIssuanceTokenCommand: CliktCommand(name = "token", help = "Get access token using authorization code from auth command") {

  val mode: CompatibilityMode by option("-m", "--mode", help = "Request body mode [oidc|ebsi_wct]").enum<CompatibilityMode>().default(CompatibilityMode.OIDC)
  val issuer_url: String by option("-i", "--issuer", help = "Issuer base URL").required()
  val code: String? by option("-c", "--code", help = "Code retrieved through previously executed auth command. Alternatively can be read from redirect-uri if specified")
  val redirect_uri: String by option("-r", "--redirect-uri", help = "Redirect URI, same as in 'oidc issue auth' command, can contain ?code parameter, to read code from").default("http://blank")
  val client_id: String? by option("--client-id", help = "Client ID for authorization at the issuer API")
  val client_secret: String? by option("--client-secret", help = "Client Secret for authorization at the issuer API")

  override fun run() {
    val issuer = OIDCProvider(issuer_url, issuer_url, client_id = client_id, client_secret = client_secret)
    val authCode = code ?: OIDCUtils.getCodeFromRedirectUri(URI.create(redirect_uri))
    if(authCode == null) {
      println("Error: Code not specified")
    } else {
      val tokenResponse = issuer.ciSvc.getAccessToken(authCode, redirect_uri.substringBeforeLast("?"), mode)
      println("Access token response:")
      val jsonObj = tokenResponse.toJSONObject()
      println(jsonObj.prettyPrint())
      println()
      println("Now get the credential using:")
      println("ssikit oidc ci credential -i $issuer_url -m $mode -t ${jsonObj.get("access_token") ?: "<token>"} ${jsonObj.get("c_nonce")?.let { "-n $it" } ?: ""} -d <subject did> -s <credential schema id>")
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
    val issuer = OIDCProvider(issuer_url, issuer_url, client_id = client_id, client_secret = client_secret)

    val proof = issuer.ciSvc.generateDidProof(did, nonce)
    val c = issuer.ciSvc.getCredential(BearerAccessToken(token), did, schemaId, proof, format, mode)
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
    val verifier = OIDCProvider(verifier_url, verifier_url)
    val req = verifier.vpSvc.fetchSIOPv2Request()
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
      claims = VCClaims(vp_token = VpTokenClaim(PresentationDefinition("1", schemaIds.mapIndexed { idx, id -> InputDescriptor("$idx", schema = VCSchema(id)) }))),
      state = state
    )
    println("${client_url}?${req.toUriQueryString()}")
  }
}

class OidcVerificationParseCommand: CliktCommand(name = "parse", help = "Parse SIOP presentation request") {

  val authUrl: String? by option("-u", "--url", help = "Authentication request URL from verifier portal, or get-url / gen-url subcommands")

  override fun run() {
    val verifier = OIDCProvider("", "")
    val req = verifier.vpSvc.parseSIOPv2RequestUri(URI.create(authUrl))
    if(req == null) {
      println("Error parsing SIOP request")
    } else {
      println("Requested credentials:")
      req.claims?.vp_token?.presentation_definition?.input_descriptors?.forEach { id ->
        println("- "  + (VcTemplateManager.getTemplateList().firstOrNull { t -> VcTemplateManager.loadTemplate(t).credentialSchema?.id == id.schema?.uri } ?: "Unknown type"))
        println("Schema ID: ${id.schema?.uri}")
      }
    }
  }
}

class OidcVerificationRespondCommand: CliktCommand(name = "present", help = "Create presentation response, and post to verifier") {

  val authUrl: String? by option("-u", "--url", help = "Authentication request URL from verifier portal, or get-url / gen-url subcommands")
  val did: String by option("-d", "--did", help = "Subject DID of presented credential(s)").required()
  val credentialIds: List<String> by option("-c", "--credential-id", help = "One or multiple credential IDs to be presented").multiple(listOf())
  val mode: CompatibilityMode by option("-m", "--mode", help = "Request body mode [oidc|ebsi_wct]").enum<CompatibilityMode>().default(CompatibilityMode.OIDC)

  override fun run() {
    val verifier = OIDCProvider("", "")
    val req = verifier.vpSvc.parseSIOPv2RequestUri(URI.create(authUrl))
    val resp = verifier.vpSvc.getSIOPResponseFor(req!!, did, listOf(Custodian.getService().createPresentation(credentialIds.map { Custodian.getService().getCredential(it)!!.encode() }, did, challenge = req.nonce, expirationDate = null).toCredential() as VerifiablePresentation))
    println("Presentation response:")
    println(resp.toFormParams().prettyPrint())

    println()
    if(req.response_mode.lowercase(Locale.getDefault()).contains("post")) { // "post" or "form_post"
      val result = verifier.vpSvc.postSIOPResponse(req, resp, mode)
      println()
      println("Response:")
      println(result)
    } else {
      println("Redirect to:")
      println("${req.redirect_uri}${when(req.response_mode) { "fragment" -> "#"; else -> "?" }}${resp.toFormBody()}")
    }
  }
}
