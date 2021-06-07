package org.letstrust.services.essif

import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.letstrust.LetsTrustServices
import org.letstrust.common.readEssif
import org.letstrust.services.essif.mock.DidRegistry

private val log = KotlinLogging.logger {}

object EosService {

    val domain = "https://api.preprod.ebsi.eu"
    //val domain = "https://api.test.intebsi.xyz"

    val authorisation = "$domain/authorisation/v1"
    val onboarding = "$domain/users-onboarding/v1"

    fun authenticationRequests(): AuthRequestResponse = runBlocking {
        return@runBlocking LetsTrustServices.http.post<AuthRequestResponse>("$onboarding/authentication-requests") {
            contentType(ContentType.Application.Json)
            headers {
                append(HttpHeaders.Accept, "application/json")
            }
            body = mapOf("scope" to "ebsi users onboarding")
        }
    }

    fun authenticationResponse(idToken: String, bearerToken: String): String = runBlocking {
        return@runBlocking LetsTrustServices.http.post<String>("$onboarding/authentication-responses") {
            contentType(ContentType.Application.Json)
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.Authorization, "Bearer $bearerToken")
            }
            body = mapOf("id_token" to idToken)
        }
    }


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

    fun requestVerifiableCredential(credentialRequestUri: String): String {
        println("4. [Eos] Request V.ID")
        return EnterpriseWalletService.generateDidAuthRequest()
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

    fun getCredentials(isUserAuthenticated: Boolean = false): String {
        return if (isUserAuthenticated) {
            readEssif("vc-issuance-auth-req")
        } else {
            println("2. [Eos] [GET]/credentials")
            EnterpriseWalletService.generateDidAuthRequest()
            println("4. [Eos] 200 <DID-Auth Req>")
            println("5. [Eos] Generate QR, URI")
            // TODO: Trigger job for [GET] /sessions/{id}
            val str = EnterpriseWalletService.getSession("sessionID")
            readEssif("vc-issuance-auth-req")
        }

    }
}
