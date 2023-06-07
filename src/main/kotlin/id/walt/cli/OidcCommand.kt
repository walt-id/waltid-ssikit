package id.walt.cli

import com.beust.klaxon.JsonObject
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import com.nimbusds.oauth2.sdk.ResponseMode
import com.nimbusds.oauth2.sdk.ResponseType
import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import com.nimbusds.oauth2.sdk.util.URLUtils
import com.nimbusds.openid.connect.sdk.Nonce
import id.walt.common.KlaxonWithConverters
import id.walt.common.prettyPrint
import id.walt.credentials.w3c.PresentableCredential
import id.walt.credentials.w3c.toVerifiablePresentation
import id.walt.custodian.Custodian
import id.walt.model.dif.InputDescriptor
import id.walt.model.dif.InputDescriptorConstraints
import id.walt.model.dif.InputDescriptorField
import id.walt.model.dif.PresentationDefinition
import id.walt.model.oidc.CredentialAuthorizationDetails
import id.walt.model.oidc.IssuanceInitiationRequest
import id.walt.model.oidc.OIDCProvider
import id.walt.services.oidc.CompatibilityMode
import id.walt.services.oidc.OIDC4CIService
import id.walt.services.oidc.OIDC4VPService
import id.walt.services.oidc.OIDCUtils
import id.walt.services.oidc.OidcSchemeFixer.unescapeOpenIdScheme
import java.net.URI
import java.util.*


class OidcCommand : CliktCommand(
    name = "oidc", help = """OIDC for verifiable presentation and credential issuance
  
  OIDC commands, related to credential presentation and credential issuance through OIDC SIOPv2 specifications
"""
) {
    override fun run() {}
}

class OidcIssuanceCommand : CliktCommand(name = "ci", help = "OIDC for Credential Issuance") {
    override fun run() {}
}

class OidcVerificationCommand : CliktCommand(name = "vp", help = "OIDC for Verifiable Presentations") {
    override fun run() {}
}

class OidcIssuanceInfoCommand :
    CliktCommand(name = "info", help = "List issuer info: supported credentials and optional VP requirements") {
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

class OidcIssuanceInitiationCommand : CliktCommand(name = "initiation", help = "Parse issuance initiation request") {
    val uri: String by option("-u", "--uri", help = "OIDC4VCI issuance initiation request URI").required()

    override fun run() {
        val issuanceInitiationRequest =
            IssuanceInitiationRequest.fromQueryParams(URLUtils.parseParameters(URI.create(uri).query))
        println("Issuer: ${issuanceInitiationRequest.issuer_url}")
        println("Pre-authorized: ${issuanceInitiationRequest.isPreAuthorized}")
        println("Credential types: ${issuanceInitiationRequest.credential_types.joinToString(", ")}")
        println("---")
        if (issuanceInitiationRequest.isPreAuthorized) {
            println("Now get the access token using:")
            println(
                "ssikit oidc ci token -i ${issuanceInitiationRequest.issuer_url}" +
                        " --pre" +
                        " -c \"${issuanceInitiationRequest.pre_authorized_code}\"" +
                        " -r \"<wallet redirectUri>\"" +
                        " --client-id <optional: your_client_id>" +
                        " --client-secret <optional: your_client_secret>"
            )
        } else {
            println("Now continue with the authorization step:")
            println(
                "ssikit oidc ci auth -i ${issuanceInitiationRequest.issuer_url} " +
                        issuanceInitiationRequest.credential_types.joinToString(" ") { "-c $it" } +
                        " -r \"<wallet redirectUri>\"" +
                        " --client-id <optional: your_client_id>" +
                        " --client-secret <optional: your_client_secret>" +
                        " -n <nonce> -f <format>"
            )
        }
    }
}

class OidcIssuanceAuthCommand : CliktCommand(name = "auth", help = "OIDC issuance authorization step") {
    val mode: String by option("-m", "--mode", help = "Authorization mode [push|get|redirect]").default("push")
    val issuer_url: String by option("-i", "--issuer", help = "Issuer base URL").required()
    val nonce: String? by option(
        "-n",
        "--nonce",
        help = "Nonce for auth request, to sign with id_token returned by tokens request."
    )
    val client_id: String? by option("--client-id", help = "Client ID for authorization at the issuer API")
    val client_secret: String? by option("--client-secret", help = "Client Secret for authorization at the issuer API")
    val redirect_uri: String by option(
        "-r",
        "--redirect-uri",
        help = "Redirect URI to send with the authorization request"
    ).default("http://blank")
    val credential_types: List<String> by option(
        "-c",
        "--credential-type",
        help = "Credential type of credential to be issued, like given by issuer metadata supported_credentials"
    ).multiple(required = true)
    val format: String by option(
        "-f",
        "--format",
        help = "Desired credential format [ldp_vc, jwt_vc], default: ldp_vc"
    ).default("ldp_vc")

    override fun run() {
        val issuer = OIDC4CIService.getWithProviderMetadata(
            OIDCProvider(
                issuer_url,
                issuer_url,
                client_id = client_id,
                client_secret = client_secret
            )
        )
        val credentialDetails = credential_types.map { CredentialAuthorizationDetails(credential_type = it, format = format) }

        when (mode) {
            "get" -> {
                val redirectUri = OIDC4CIService.executeGetAuthorizationRequest(
                    issuer,
                    URI.create(redirect_uri),
                    credentialDetails,
                    nonce = nonce
                )
                println()
                println("Client redirect URI:")
                println(redirectUri)
                println()
                println("Now get the token using:")
                println("ssikit oidc ci token -i $issuer_url" +
                        (client_id?.let { " --client-id $client_id" } ?: "") +
                        (client_secret?.let { " --client-secret $client_secret" } ?: "") +
                        " -m ebsi_wct -r \"$redirectUri\"")
            }

            "redirect" -> {
                val userAgentUri = OIDC4CIService.getUserAgentAuthorizationURL(
                    issuer,
                    URI.create(redirect_uri),
                    credentialDetails,
                    nonce = nonce
                )
                println()
                println("Point your browser to this address and authorize with the issuer:")
                println(userAgentUri)
                println()
                println("Then paste redirection url from browser to this command to retrieve the access token:")
                println("ssikit oidc ci token -i $issuer_url" +
                        (client_id?.let { " --client-id $client_id" } ?: "") +
                        (client_secret?.let { " --client-secret $client_secret" } ?: "") +
                        " -r <url from browser>")

            }

            else -> {
                val userAgentUri = OIDC4CIService.executePushedAuthorizationRequest(
                    issuer,
                    URI.create(redirect_uri),
                    credentialDetails,
                    nonce = nonce
                )
                println()
                println("Point your browser to this address and authorize with the issuer:")
                println(userAgentUri)
                println()
                println("Then paste redirection url from browser to this command to retrieve the access token:")
                println("ssikit oidc ci token -i $issuer_url" +
                        (client_id?.let { " --client-id $client_id" } ?: "") +
                        (client_secret?.let { " --client-secret $client_secret" } ?: "") +
                        " -r <url from browser>")
            }
        }
    }
}

class OidcIssuanceTokenCommand :
    CliktCommand(name = "token", help = "Get access token using authorization code from auth command") {

    val issuer_url: String by option("-i", "--issuer", help = "Issuer base URL").required()
    val code: String? by option(
        "-c",
        "--code",
        help = "Code retrieved through previously executed auth command. Alternatively can be read from redirect-uri if specified"
    )
    val redirect_uri: String? by option(
        "-r",
        "--redirect-uri",
        help = "Redirect URI, same as in 'oidc issue auth' command, can contain ?code parameter, to read code from"
    )
    val client_id: String? by option("--client-id", help = "Client ID for authorization at the issuer API")
    val client_secret: String? by option("--client-secret", help = "Client Secret for authorization at the issuer API")
    val isPreAuthorized: Boolean by option(
        "--pre",
        "--pre-authz",
        help = "Set if this is a pre-authorized code, from an issuance initiation request, requires --code parameter."
    ).flag(default = false)
    val userPin: String? by option(
        "--pin",
        "--user-pin",
        help = "Optional user PIN for pre-authorized flow, if required by issuer"
    )

    override fun run() {
        val issuer = OIDC4CIService.getWithProviderMetadata(
            OIDCProvider(
                issuer_url,
                issuer_url,
                client_id = client_id,
                client_secret = client_secret
            )
        )
        val authCode = code ?: OIDCUtils.getCodeFromRedirectUri(URI.create(redirect_uri))
        if (authCode == null) {
            println("Error: Code not specified")
        } else {
            val tokenResponse =
                OIDC4CIService.getAccessToken(
                    issuer, authCode,
                    redirect_uri?.substringBeforeLast("?"), isPreAuthorized, userPin
                )
            println("Access token response:")
            val jsonObj = tokenResponse.toJSONObject()
            println(jsonObj.prettyPrint())
            println()
            println("Now get the credential using:")
            println(
                "ssikit oidc ci credential -i $issuer_url -t ${jsonObj["access_token"] ?: "<token>"} ${
                    jsonObj["c_nonce"]?.let { "-n $it" } ?: ""
                } -d <subject did> -s <credential schema id>")
        }
    }
}

class OidcIssuanceCredentialCommand :
    CliktCommand(name = "credential", help = "Get credential using access token from token command") {

    val issuer_url: String by option("-i", "--issuer", help = "Issuer base URL").required()
    val token: String by option(
        "-t",
        "--token",
        help = "Access token retrieved through previously executed token command."
    ).required()
    val nonce: String by option(
        "-n",
        "--nonce",
        help = "Nonce retrieved through previously executed token command, for proving did possession."
    ).required()
    val did: String by option("-d", "--did", help = "Subject DID to issue credential for").required()
    val schemaId: String by option(
        "-c",
        "--credential-type",
        help = "Credential type of credential to be issued. Must correspond to one credential type specified in previously called auth command"
    ).required()
    val format: String by option(
        "-f",
        "--format",
        help = "Desired credential format [ldp_vc, jwt_vc], default: ldp_vc"
    ).default("ldp_vc")
    val client_id: String? by option("--client-id", help = "Client ID for authorization at the issuer API")
    val client_secret: String? by option("--client-secret", help = "Client Secret for authorization at the issuer API")
    val save: Boolean by option("--save", help = "Store credential in custodial credential store, default: false").flag()

    override fun run() {
        val issuer = OIDC4CIService.getWithProviderMetadata(
            OIDCProvider(
                issuer_url,
                issuer_url,
                client_id = client_id,
                client_secret = client_secret
            )
        )

        val proof = OIDC4CIService.generateDidProof(issuer, did, nonce)
        val c = OIDC4CIService.getCredential(issuer, BearerAccessToken(token), schemaId, proof, format)
        if (c == null)
            println("Error: no credential received")
        else {
            println(c.prettyPrint())

            if (save) {
                c.id = c.id ?: UUID.randomUUID().toString()
                Custodian.getService().storeCredential(c.id!!, c)
                println()
                println("Stored as ${c.id}")
            }
        }
    }
}

class OidcVerificationGetUrlCommand :
    CliktCommand(name = "get-url", help = "Get authentication request url, to initiate verification session") {

    val verifier_url: String by option("-v", "--verifier", help = "Verifier base URL").required()
    val client_url: String by option(
        "-c",
        "--client-url",
        help = "Base URL of client, e.g. Wallet, default: openid://"
    ).default("openid://")

    override fun run() {
        val verifier = OIDCProvider(verifier_url, verifier_url)
        val req = OIDC4VPService.fetchOIDC4VPRequest(verifier)
        if (req == null) {
            println("<Error fetching redirection url>")
        } else {
            println("$client_url?${req.toQueryString()}")
        }
    }
}

class OidcVerificationGenUrlCommand :
    CliktCommand(name = "gen-url", help = "Get authentication request url, to initiate verification session") {

    val verifier_url: String by option("-v", "--verifier", help = "Verifier base URL").required()
    val verifier_path: String by option(
        "-p",
        "--path",
        help = "API path relative verifier url, to redirect the response to"
    ).required()
    val client_url: String by option(
        "-c",
        "--client-url",
        help = "Base URL of client, e.g. Wallet, default: openid://"
    ).default("openid://")
    val response_type: String by option("--response-type", help = "Response type, default: vp_token").default("vp_token")
    val response_mode: String by option("--response-mode", help = "Response mode, default: fragment").default("fragment")
    val nonce: String? by option("-n", "--nonce", help = "Nonce, default: auto-generated")
    val scope: String? by option("--scope", help = "Set OIDC scope defining pre-defined presentation definition")
    val presentationDefinitionUrl: String? by option("--presentation-definition-url", help = "URL to presentation definition")
    val credentialTypes: List<String>? by option(
        "--credential-type",
        help = "Credential types to request, supports multiple"
    ).multiple(required = false)
    val state: String? by option("--state", help = "State to be passed through")

    override fun run() {
        val req = OIDC4VPService.createOIDC4VPRequest(
            wallet_url = client_url,
            redirect_uri = URI.create("${verifier_url.trimEnd('/')}/${verifier_path.trimStart('/')}"),
            nonce = nonce?.let { Nonce(it) } ?: Nonce(),
            response_type = ResponseType.parse(response_type),
            response_mode = ResponseMode(response_mode),
            scope = scope?.let { Scope(scope) },
            presentation_definition = if (scope.isNullOrEmpty() && presentationDefinitionUrl.isNullOrEmpty()) {
                PresentationDefinition("1",
                    input_descriptors = credentialTypes?.map { credType ->
                        InputDescriptor(
                            "1",
                            constraints = InputDescriptorConstraints(
                                listOf(
                                    InputDescriptorField(
                                        listOf("$.type"),
                                        "1",
                                        filter = JsonObject(mapOf("const" to credType))
                                    )
                                )
                            )
                        )
                    } ?: listOf())
            } else {
                null
            },
            presentation_definition_uri = presentationDefinitionUrl?.let { URI.create(it) },
            state = state?.let { State(it) }
        )
        println("${req.toURI().unescapeOpenIdScheme()}")
    }
}

class OidcVerificationParseCommand : CliktCommand(name = "parse", help = "Parse SIOP presentation request") {

    val authUrl: String by option(
        "-u",
        "--url",
        help = "Authentication request URL from verifier portal, or get-url / gen-url subcommands"
    ).required()

    val listCredentials: Boolean by option(
        "-l",
        "--list-credentials",
        help = "List available credentials, matching presentation request"
    ).flag(default = false)

    override fun run() {
        val req = OIDC4VPService.parseOIDC4VPRequestUri(URI.create(authUrl))
        if (req == null) {
            println("Error parsing SIOP request")
        } else {
            val presentationDefinition = OIDC4VPService.getPresentationDefinition(req)
            println("Presentation requirements:")
            println(KlaxonWithConverters().toJsonString(presentationDefinition).prettyPrint())
            if (listCredentials) {
                println("----------------------------")
                println("Matching credentials by input descriptor id:")
                val credentialMap = OIDCUtils.findCredentialsFor(OIDC4VPService.getPresentationDefinition(req))
                credentialMap.keys.forEach { inputDescriptor ->
                    println("$inputDescriptor: ${credentialMap[inputDescriptor]?.joinToString(", ") ?: "<none>"}")
                }
            }
        }
    }
}

class OidcVerificationRespondCommand :
    CliktCommand(name = "present", help = "Create presentation response, and post to verifier") {

    val authUrl: String? by option(
        "-u",
        "--url",
        help = "Authentication request URL from verifier portal, or get-url / gen-url subcommands"
    )
    val did: String by option("-d", "--did", help = "Subject DID of presented credential(s)").required()
    val credentialIds: List<String> by option(
        "-c",
        "--credential-id",
        help = "One or multiple credential IDs to be presented"
    ).multiple(listOf())
    val mode: CompatibilityMode by option("-m", "--mode", help = "Request body mode [oidc|ebsi_wct]").enum<CompatibilityMode>()
        .default(CompatibilityMode.OIDC)

    override fun run() {
        val req = OIDC4VPService.parseOIDC4VPRequestUri(URI.create(authUrl))
        val nonce = req.getCustomParameter("nonce")?.firstOrNull()
        val vp = Custodian.getService().createPresentation(
            vcs = credentialIds.map { Custodian.getService().getCredential(it)?.let { PresentableCredential(it) } ?: throw Exception("Credential with given ID $it not found") },
            holderDid = did,
            challenge = nonce,
        ).toVerifiablePresentation()
        val resp = OIDC4VPService.getSIOPResponseFor(req, did, listOf(vp))
        println("Presentation response:")
        println(resp.toFormParams().prettyPrint())

        println()
        if (setOf(ResponseMode.FORM_POST, ResponseMode("post")).contains(req.responseMode)) { // "post" or "form_post"
            val result = OIDC4VPService.postSIOPResponse(req, resp, mode)
            println()
            println("Response:")
            println(result)
        } else {
            println("Redirect to:")
            println(
                "${req.redirectionURI}${
                    when (req.responseMode) {
                        ResponseMode.FRAGMENT -> "#"; else -> "?"
                    }
                }${resp.toFormBody()}"
            )
        }
    }
}
