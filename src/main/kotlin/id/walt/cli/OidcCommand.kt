package id.walt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.nimbusds.oauth2.sdk.AuthorizationRequest
import com.nimbusds.oauth2.sdk.ResponseMode
import com.nimbusds.oauth2.sdk.ResponseType
import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import com.nimbusds.openid.connect.sdk.Nonce
import id.walt.common.prettyPrint
import id.walt.custodian.Custodian
import id.walt.model.dif.*
import id.walt.model.oidc.*
import id.walt.services.oidc.CompatibilityMode
import id.walt.services.oidc.OIDC4CIService
import id.walt.services.oidc.OIDC4VPService
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
    val issuer = OIDC4CIService.getWithProviderMetadata(OIDCProvider(issuer_url, issuer_url))

    println("###")
    println("Issuer:")
    println(OIDC4CIService.getIssuerInfo(issuer)?.display?.firstOrNull()?.name ?: "<No issuer information provided>")
    println("---")
    OIDC4CIService.getSupportedCredentials(issuer).forEach { supported_cred ->
      println("Issuable credentials:")
      println("- ${supported_cred.key}")
      println("---")
    }
  }
}

class OidcIssuanceNonceCommand: CliktCommand(name = "nonce", help = "Get nonce from issuer for required verifiable presentation") {
  val issuer_url: String by option("-i", "--issuer", help = "Issuer base URL").required()
  val client_id: String? by option("--client-id", help = "Client ID for authorization at the issuer API")
  val client_secret: String? by option("--client-secret", help = "Client Secret for authorization at the issuer API")

  override fun run() {
    val issuer = OIDC4CIService.getWithProviderMetadata(OIDCProvider(issuer_url, issuer_url, client_id = client_id, client_secret = client_secret))
    println(klaxon.toJsonString(OIDC4CIService.getNonce(issuer)).prettyPrint())
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
    val issuer = OIDC4CIService.getWithProviderMetadata(OIDCProvider(issuer_url, issuer_url, client_id = client_id, client_secret = client_secret))
    val credentialClaims = schema_ids.map { CredentialClaim(type = it, manifest_id = null) }
    val vp_token = vp?.let {
      if(File(vp).exists()) {
        File(vp).readText(StandardCharsets.UTF_8).toCredential() as VerifiablePresentation
      } else
        null
    }?.let { listOf(it) }
    when(mode) {
        "get" -> {
          val redirectUri = OIDC4CIService.executeGetAuthorizationRequest(issuer, URI.create(redirect_uri), credentialClaims, nonce = nonce, vp_token = vp_token)
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
          val userAgentUri = OIDC4CIService.getUserAgentAuthorizationURL(issuer, URI.create(redirect_uri), credentialClaims, nonce = nonce, vp_token = vp_token)
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
          val userAgentUri = OIDC4CIService.executePushedAuthorizationRequest(issuer, URI.create(redirect_uri), credentialClaims, nonce = nonce, vp_token = vp_token)
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
    val issuer = OIDC4CIService.getWithProviderMetadata(OIDCProvider(issuer_url, issuer_url, client_id = client_id, client_secret = client_secret))
    val authCode = code ?: OIDCUtils.getCodeFromRedirectUri(URI.create(redirect_uri))
    if(authCode == null) {
      println("Error: Code not specified")
    } else {
      val tokenResponse = OIDC4CIService.getAccessToken(issuer, authCode, redirect_uri.substringBeforeLast("?"), mode)
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
    val issuer = OIDC4CIService.getWithProviderMetadata(OIDCProvider(issuer_url, issuer_url, client_id = client_id, client_secret = client_secret))

    val proof = OIDC4CIService.generateDidProof(did, nonce)
    val c = OIDC4CIService.getCredential(issuer, BearerAccessToken(token), did, schemaId, proof, format, mode)
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
    val req = OIDC4VPService.fetchOIDC4VPRequest(verifier)
    if(req == null) {
      println("<Error fetching redirection url>")
    } else {
      println("$client_url?${req.toQueryString()}")
    }
  }
}

class OidcVerificationGenUrlCommand: CliktCommand(name = "gen-url", help = "Get authentication request url, to initiate verification session") {

  val verifier_url: String by option("-v", "--verifier", help = "Verifier base URL").required()
  val verifier_path: String by option("-p", "--path", help = "API path relative verifier url, to redirect the response to").required()
  val client_url: String by option("-c", "--client-url", help = "Base URL of client, e.g. Wallet, default: openid:///").default("openid:///")
  val response_type: String by option("--response-type", help = "Response type, default: vp_token").default("vp_token")
  val response_mode: String by option("--response-mode", help = "Response mode, default: fragment").default("fragment")
  val nonce: String? by option("-n", "--nonce", help = "Nonce, default: auto-generated")
  val scope: String? by option("--scope", help = "Set OIDC scope defining pre-defined presentation definition")
  val presentationDefinitionUrl: String? by option("--presentation-definition-url", help = "URL to presentation definition")
  val credentialTypes: List<String>? by option("--credential-type", help = "Credential types to request, supports multiple").multiple(required = false)
  val state: String? by option("--state", help = "State to be passed through")

  override fun run() {
    val req = OIDC4VPService.createOIDC4VPRequest(
      wallet_url = URI.create(client_url),
      redirect_uri = URI.create("${verifier_url.trimEnd('/')}/${verifier_path.trimStart('/')}"),
      nonce = nonce?.let { Nonce(it) } ?: Nonce(),
      response_type = ResponseType.parse(response_type),
      response_mode = ResponseMode(response_mode),
      scope = Scope(scope),
      presentation_definition = if(scope.isNullOrEmpty() && presentationDefinitionUrl.isNullOrEmpty()) {
        PresentationDefinition("1",
          input_descriptors = credentialTypes?.map { credType ->
            InputDescriptor("1",
              constraints = InputDescriptorConstraints(
                listOf(InputDescriptorField(listOf("$.type"), "1", filter = mapOf("const" to credType)))
        ))} ?: listOf())
      } else { null },
      presentation_definition_uri = presentationDefinitionUrl?.let { URI.create(it) },
      state = state?.let { State(it) }
    )
    println("${req.toURI()}")
  }
}

class OidcVerificationParseCommand: CliktCommand(name = "parse", help = "Parse SIOP presentation request") {

  val authUrl: String? by option("-u", "--url", help = "Authentication request URL from verifier portal, or get-url / gen-url subcommands")

  override fun run() {
    val verifier = OIDCProvider("", "")
    val req = OIDC4VPService.parseOIDC4VPRequestUri(URI.create(authUrl))
    if(req == null) {
      println("Error parsing SIOP request")
    } else {
      val presentationDefinition = OIDC4VPService.getPresentationDefinition(req)
      println("Presentation requirements:")
      println(klaxon.toJsonString(presentationDefinition).prettyPrint())
    }
  }
}

class OidcVerificationRespondCommand: CliktCommand(name = "present", help = "Create presentation response, and post to verifier") {

  val authUrl: String? by option("-u", "--url", help = "Authentication request URL from verifier portal, or get-url / gen-url subcommands")
  val did: String by option("-d", "--did", help = "Subject DID of presented credential(s)").required()
  val credentialIds: List<String> by option("-c", "--credential-id", help = "One or multiple credential IDs to be presented").multiple(listOf())
  val mode: CompatibilityMode by option("-m", "--mode", help = "Request body mode [oidc|ebsi_wct]").enum<CompatibilityMode>().default(CompatibilityMode.OIDC)

  override fun run() {
    val req = OIDC4VPService.parseOIDC4VPRequestUri(URI.create(authUrl))
    val nonce = req.getCustomParameter("nonce")?.firstOrNull()
    val vp = Custodian.getService().createPresentation(credentialIds.map { Custodian.getService().getCredential(it)!!.encode() }, did, challenge = nonce, expirationDate = null).toCredential() as VerifiablePresentation
    val resp = OIDC4VPService.getSIOPResponseFor(req!!, did, listOf(vp))
    println("Presentation response:")
    println(resp.toFormParams().prettyPrint())

    println()
    if(setOf(ResponseMode.FORM_POST, ResponseMode("post")).contains(req.responseMode)) { // "post" or "form_post"
      val result = OIDC4VPService.postSIOPResponse(req, resp, mode)
      println()
      println("Response:")
      println(result)
    } else {
      println("Redirect to:")
      println("${req.redirectionURI}${when(req.responseMode) { ResponseMode.FRAGMENT -> "#"; else -> "?" }}${resp.toFormBody()}")
    }
  }
}
