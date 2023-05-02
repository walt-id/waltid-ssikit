package id.walt.services.ecosystems.essif

import com.beust.klaxon.Klaxon
import id.walt.common.KlaxonWithConverters
import id.walt.common.readEssif
import id.walt.common.resolveContent
import id.walt.model.AttributeRecord
import id.walt.model.AuthRequestResponse
import id.walt.model.TrustedIssuer
import id.walt.model.TrustedIssuerType
import id.walt.services.WaltIdServices
import id.walt.services.ecosystems.essif.didebsi.EBSI_ENV_URL
import id.walt.services.ecosystems.essif.enterprisewallet.EnterpriseWalletService
import id.walt.services.ecosystems.essif.mock.DidRegistry
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

object TrustedIssuerClient {

    // TODO: move to config file
    val domain = EBSI_ENV_URL
    //val domain = "https://api.test.intebsi.xyz"

    val authorisation = "$domain/authorisation/v2"
    val onboarding = "$domain/users-onboarding/v2"
    const val apiVersion = "v3"
    const val schemaApiVersion = "v2"
    const val trustedIssuerPath = "trusted-issuers-registry/$apiVersion/issuers"
    const val trustedSchemaPath = "trusted-schemas-registry/$schemaApiVersion/schemas"

    private const val attributesPath = "attributes"
    private const val trustedIssuerUrl = "http://localhost:7001/v2/trusted-issuer"
    private val enterpriseWalletService = EnterpriseWalletService.getService()
    private val httpClient = WaltIdServices.httpNoAuth

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Used for VC exchange flows

    // Stubs
//    fun generateAuthenticationRequest(): String {
//        return EssifServer.generateAuthenticationRequest()
//    }
//
//    fun openSession(authResp: String): String {
//        return EssifServer.openSession(authResp)
//    }

    fun generateAuthenticationRequest(): String = runBlocking {
        return@runBlocking httpClient.post("$trustedIssuerUrl/generateAuthenticationRequest") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }.bodyAsText()
    }


    fun openSession(authResp: String): String = runBlocking {
        return@runBlocking httpClient.post("$trustedIssuerUrl/openSession") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(authResp)
        }.body<String>()
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Used for registering DID EBSI

    fun authenticationRequests(): AuthRequestResponse = runBlocking {
        return@runBlocking httpClient.post("$onboarding/authentication-requests") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(mapOf("scope" to "ebsi users onboarding"))
        }.body<AuthRequestResponse>()
    }

    fun authenticationResponse(idToken: String, bearerToken: String): String = runBlocking {
        return@runBlocking httpClient.post("$onboarding/authentication-responses") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            headers {
                append(HttpHeaders.Authorization, "Bearer $bearerToken")
            }
            setBody(mapOf("id_token" to idToken))
        }.bodyAsText()
    }

    fun siopSession(idToken: String, vpToken: String): String = runBlocking {
        return@runBlocking httpClient.post("$authorisation/siop-sessions") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(mapOf("id_token" to idToken, "vp_token" to vpToken))
        }.bodyAsText()
    }

    fun siopSessionBearer(idToken: String, bearerToken: String): String = runBlocking {
        return@runBlocking httpClient.post("$authorisation/siop-sessions") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            headers {
                append(HttpHeaders.Authorization, "Bearer $bearerToken")
            }
            setBody(mapOf("id_token" to idToken))
        }.bodyAsText()
    }

    // GET /issuers/{did}
    // returns trusted issuer record
    fun getIssuerRaw(did: String, registryAddress: String = "$domain/$trustedIssuerPath"): String = runBlocking {
        log.debug { "Getting trusted issuer with DID $did" }
        val trustedIssuer: String = resolveContent("$registryAddress/$did")
        log.debug { trustedIssuer }
        return@runBlocking trustedIssuer
    }

    fun getIssuer(did: String, registryAddress: String): TrustedIssuer =
        Klaxon().parse<TrustedIssuer>(getIssuerRaw(did, registryAddress))!!

    fun getIssuer(did: String): TrustedIssuer = getIssuer(did, "$domain/$trustedIssuerPath")

    @Deprecated(
        "Mock solution for ebsi registry. To be removed",
        ReplaceWith("getIssuer(did), getIssuer(did, registryAddress)")
    )
    fun getIssuer(type: TrustedIssuerType): TrustedIssuer = runBlocking {
        when (type) {
            TrustedIssuerType.TI -> "https://raw.githubusercontent.com/walt-id/waltid-ssikit/main/src/test/resources/ebsi/trusted-issuer-chain/ti-tir-record.json"
            TrustedIssuerType.TAO -> "https://raw.githubusercontent.com/walt-id/waltid-ssikit/main/src/test/resources/ebsi/trusted-issuer-chain/tao-tir-record.json"
            else -> ""
        }.let {
            KlaxonWithConverters().parse(resolveContent(it))!!
        }
    }

    fun getAttribute(did: String, attributeId: String) =
        getAttribute("$domain/$trustedIssuerPath/$did/$attributesPath/$attributeId")

    fun getAttribute(url: String) = resolveContent(url).let {
        KlaxonWithConverters().parse<AttributeRecord>(it)!!
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //TODO: the methods below are stubbed - to be considered

    // POST /onboards
    // returns DID ownership
    fun onboards(): String {
        println("6. [Eos] Request DID ownership")
        return readEssif("onboarding-onboards-resp")
    }

    fun signedChallenge(signedChallenge: String): String {

        val header = readEssif("onboarding-onboards-callback-req-header")
        val body = readEssif("onboarding-onboards-callback-req-body")

        log.debug { "header: $header" }
        log.debug { "body: $body" }

        println("8. [Eos] Validate DID Document")
        println("9. [Eos] GET /identifiers/{did}")
        DidRegistry.get("did")
        println("10. [Eos] 404 Not found")
        println("11. [Eos] Generate Verifiable Authorization")
        val verifiableAuthorization = readEssif("onboarding-onboards-callback-resp")
        return verifiableAuthorization
    }

    fun requestVerifiableCredential(): String {
        println("4. [Eos] Request V.ID")
        return enterpriseWalletService.generateDidAuthRequest()
    }

    fun requestCredentialUri(): String {
        println("2 [Eos] Request Credential (QR, URI, ...)")
        return "new session - QR/URI"
    }

    fun didOwnershipResponse(didOwnershipResp: String): String {
        println("8. [Eos] Response DID ownership")
        log.debug { "didOwnershipResp: $didOwnershipResp" }

        // TODO: move following call to:
        //EnterpriseWalletService.validateDidAuthResponse(didOwnershipResp)

        println("9. [Eos] Validate DID ownership")
        val didOwnershipRespHeader = readEssif("onboarding-did-ownership-resp-header")
        log.debug { "didOwnershipRespHeader: $didOwnershipRespHeader" }
        val didOwnershipRespBody = readEssif("onboarding-did-ownership-resp-body")
        log.debug { "didOwnershipRespBody: $didOwnershipRespBody" }
        val vIdRequestOkResp = readEssif("onboarding-vid-req-ok")

        return vIdRequestOkResp
    }

    fun getCredential(id: String): String {
        println("12. [Eos] [GET]/credentials")
        return readEssif("onboarding-vid")
    }

    fun getCredentials(isUserAuthenticated: Boolean): String {
        return if (isUserAuthenticated) {
            readEssif("vc-issuance-auth-req")
        } else {
            println("2. [Eos] [GET]/credentials")
            enterpriseWalletService.generateDidAuthRequest()
            println("4. [Eos] 200 <DID-Auth Req>")
            println("5. [Eos] Generate QR, URI")
            // TODO: Trigger job for [GET] /sessions/{id}
            val str = enterpriseWalletService.getSession("sessionID")
            readEssif("vc-issuance-auth-req")
        }

    }
}
