package id.walt.services.essif

import com.beust.klaxon.Klaxon
import id.walt.common.readEssif
import id.walt.model.AuthRequestResponse
import id.walt.model.TrustedIssuer
import id.walt.services.WaltIdServices.httpLogging
import id.walt.services.essif.enterprisewallet.EnterpriseWalletService
import id.walt.services.essif.mock.DidRegistry
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

object TrustedIssuerClient {

    // TODO: move to config file
    val domain = "https://api.preprod.ebsi.eu"
    //val domain = "https://api.test.intebsi.xyz"

    val authorisation = "$domain/authorisation/v1"
    val onboarding = "$domain/users-onboarding/v1"
    val trustedIssuerUrl = "http://localhost:7001/v2/trusted-issuer"


    val http = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
        if (httpLogging) {
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.HEADERS
            }
        }

        defaultRequest {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }
    }


    private val enterpriseWalletService = EnterpriseWalletService.getService()

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
        http.post("$trustedIssuerUrl/generateAuthenticationRequest")
    }


    fun openSession(authResp: String): String = runBlocking {
        http.post("$trustedIssuerUrl/openSession", body = authResp)
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Used for registering DID EBSI

    fun authenticationRequests(): AuthRequestResponse = runBlocking {
        http.post("$onboarding/authentication-requests", body = mapOf("scope" to "ebsi users onboarding"))
    }

    fun authenticationResponse(idToken: String, bearerToken: String): String = runBlocking {
        http.post("$onboarding/authentication-responses", body = mapOf("id_token" to idToken)) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $bearerToken")
            }
        }
    }

    fun siopSession(idToken: String): String = runBlocking {
        http.post("$authorisation/siop-sessions", body = mapOf("id_token" to idToken))
    }

    fun siopSessionBearer(idToken: String, bearerToken: String): String = runBlocking {
        http.post("$authorisation/siop-sessions",  body = mapOf("id_token" to idToken)) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $bearerToken")
            }

        }
    }

    // GET /issuers/{did}
    // returns trusted issuer record
    fun getIssuerRaw(did: String): String = runBlocking {
        log.debug { "Getting trusted issuer with DID $did" }

        val trustedIssuer: String = http.get("https://api.preprod.ebsi.eu/trusted-issuers-registry/v2/issuers/$did")

        log.debug { trustedIssuer }

        return@runBlocking trustedIssuer
    }

    fun getIssuer(did: String): TrustedIssuer = runBlocking {
        log.debug { "Getting trusted issuer with DID $did" }

        val trustedIssuer: String = http.get("https://api.preprod.ebsi.eu/trusted-issuers-registry/v2/issuers/$did")

        log.debug { trustedIssuer }

        return@runBlocking Klaxon().parse<TrustedIssuer>(trustedIssuer)!!
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
