package id.walt.services.ecosystems.essif

import id.walt.common.readEssifBearerToken
import id.walt.crypto.JwtUtils
import id.walt.services.WaltIdServices
import id.walt.services.context.ContextManager
import id.walt.services.ecosystems.essif.didebsi.DidEbsiService
import id.walt.services.ecosystems.essif.enterprisewallet.EnterpriseWalletService
import id.walt.services.ecosystems.essif.mock.RelyingParty
import id.walt.services.ecosystems.essif.timestamp.Timestamp
import id.walt.services.ecosystems.essif.timestamp.WaltIdTimestampService
import id.walt.services.ecosystems.essif.userwallet.UserWalletService
import id.walt.services.hkvstore.HKVKey
import mu.KotlinLogging
import java.io.File

object EssifClient {

    private val log = KotlinLogging.logger {}

    val bearerTokenFile = File("${WaltIdServices.ebsiDir}bearer-token.txt")

    val verifiableAuthorizationFile = "verifiable-authorization.json"
    val verifiablePresentationFile = "verifiable-presentation.json"
    val ake1EncFile = "ake1_enc.json"
    val ebsiAccessTokenFile = "ebsi_access_token.json"


    private val didEbsiService = DidEbsiService.getService()
    private val enterpriseWalletService = EnterpriseWalletService.getService()


    // https://ec.europa.eu/cefdigital/wiki/display/BLOCKCHAININT/2.+Main+Flow%3A+VC-Request+-+Onboarding+Flow
    fun onboard(did: String, token: String? = null): String {

        log.debug { "Running ESSIF onboarding flow ..." }

        ///////////////////////////////////////////////////////////////////////////
        // Prerequisite: The Legal Entity (LE) or the Natural Person (NP) the must
        // be authenticated and authorized by the classical way before triggering
        // the ESSIF onboarding flow. The received bearer token must be copied in
        // file: bearer-token.txt
        ///////////////////////////////////////////////////////////////////////////

        val bearerToken = token ?: readEssifBearerToken()

        log.debug { "Loaded bearer token from ${bearerTokenFile.absolutePath}." }
        log.debug { "Loaded bearer token $bearerToken." }

        JwtUtils.getJwtExpirationIfExpired(bearerToken).let {
            if (it != null) {
                throw IllegalArgumentException(JwtUtils.getJwtExpirationMessageIfExpired(bearerToken))
            }
        }

        ///////////////////////////////////////////////////////////////////////////
        // Requesting the DID Auth Request from the ESSIF Onboarding Service (EOS)
        ///////////////////////////////////////////////////////////////////////////

        log.debug { "Requesting a Verifiable ID from ESSIF Onboarding Service (EOS)" }

        val authRequestResponse = TrustedIssuerClient.authenticationRequests()

        log.debug { "AuthRequestResponse:\n$authRequestResponse" }

        val didAuthRequest = enterpriseWalletService.parseDidAuthRequest(authRequestResponse)

        log.debug { "DidAuthRequest:\n$didAuthRequest" }

        ///////////////////////////////////////////////////////////////////////////
        // Constructing and returning the DID Auth Response
        ///////////////////////////////////////////////////////////////////////////

        val idToken =
            enterpriseWalletService.constructAuthResponseJwt(did, didAuthRequest.client_id, didAuthRequest.nonce)

        val verifiableAuthorization = TrustedIssuerClient.authenticationResponse(idToken, bearerToken)

        ///////////////////////////////////////////////////////////////////////////
        // Storing the received Verifiable Authorization
        ///////////////////////////////////////////////////////////////////////////

        log.debug { "Verifiable Authorization received:\n${verifiableAuthorization}" }


        ContextManager.hkvStore.put(
            HKVKey("ebsi", did.substringAfterLast(":"), verifiableAuthorizationFile),
            verifiableAuthorization
        )

        return verifiableAuthorization

    }

    // https://ec.europa.eu/cefdigital/wiki/display/BLOCKCHAININT/2.+Authorization+API
    fun authApi(did: String) {

        log.debug { "ESSIF Authorization API flow started" }

//        ///////////////////////////////////////////////////////////////////////////
//        // Prerequisite:
//        // - Bearer token must be available
//        // - Verifiable Authorization must be previously installed by running
//        //   ESSIF onboarding flow (DID registration)
//        ///////////////////////////////////////////////////////////////////////////
//
//        log.debug { "Loading Verifiable Authorization from file: ${verifiableAuthorizationFile.absolutePath}." }
//
//        val verifiableAuthorization = readWhenContent(verifiableAuthorizationFile)
//
//        val bearerToken = readBearerToken()
//
//        log.debug { "Loaded bearer token from ${bearerTokenFile.absolutePath}." }
//
//        UserWalletService.siopSession(did, verifiableAuthorization, bearerToken)


        ///////////////////////////////////////////////////////////////////////////
        // Run authentication protocol (DID Auth + Authenticated Key Exchange Protocol)
        // and receive JWT Access Token.
        ///////////////////////////////////////////////////////////////////////////

        val accessToken = UserWalletService.requestAccessToken(did)

        ///////////////////////////////////////////////////////////////////////////
        // Storing the received Access Token
        ///////////////////////////////////////////////////////////////////////////

        log.debug { "EBSI Access Token received:\n${accessToken}" }

        ContextManager.hkvStore.put(HKVKey("ebsi", did.substringAfterLast(":"), ebsiAccessTokenFile), accessToken)

        ///////////////////////////////////////////////////////////////////////////
        // Protected resource can now be accessed
        ///////////////////////////////////////////////////////////////////////////

        //UserWalletService.accessProtectedResource(accessToken) // e.g updateDID, revoke VC
    }

    fun registerDid(did: String, ethKeyAlias: String) {
        val maxTries = 6

        for (i in 1 until maxTries) {
            try {
                return didEbsiService.registerDid(did, ethKeyAlias)
            } catch (e: Exception) {
                log.debug { "Trying register DID EBSI failed (fail count: $i): did=$did, ethKeyAlias=$ethKeyAlias" }
                log.debug { e }
            }
        }

        throw IllegalStateException("Could not register DID (after $maxTries tries to contact EBSI)!")
    }

    // https://ec.europa.eu/cefdigital/wiki/display/BLOCKCHAININT/VC-Issuance+Flow
    fun vcIssuance() {
        println("Credential issuance from a Legal Entity (EOS/Trusted Issuer) to a Natural Person.")

        ///////////////////////////////////////////////////////////////////////////
        // Prerequisite: The Natural Person (NP) must be authenticated and authorized by the
        // classical method of the Trusted Issuer before triggering the credential issuance flow.
        ///////////////////////////////////////////////////////////////////////////

        println("1 Request VC (Manually)")

        ///////////////////////////////////////////////////////////////////////////
        // The ESSIF Onboarding Service / Trusted Issuer generates the DID Auth Request.
        // The request looks as follows:
        //
        // {
        //    "uri": "openid://?response_type=id_token&client_id=https%3A%2F%2Frp.example.com%2Fcb&scope=openid%20did_authn&request=<authentication-request-JWS>",
        //    "callback": "https://ti.example.com/callback"
        //}
        // whereas the JWT header contains the DID key of the issuer:
        // {
        //  "alg": "ES256K",
        //  "typ": "JWT",
        //  "jwk": "{DID key of the issuer}",
        // }
        // and the JWT body has the following structure:
        // {
        //  "scope": "openid did_authn",
        //  "iss": "did:ebsi:0x416e6e6162656c2e4c65652e452d412d506f652e",
        //  "response_type": "id_token",
        //  "client_id": "<redirect-uri>",
        //  "nonce": "<random-nonce>",
        //  "registration": {
        //    "redirect_uris": ["https://app.ebsi.xyz"],
        //    "response_types": "id_token",
        //    "id_token_signed_response_alg": ["RS256", "ES256", "ES256K", "EdDSA"],
        //    "request_object_signing_alg": ["RS256", "ES256", "ES256K", "EdDSA"],
        //    "access_token_signing_alg": ["RS256", "ES256", "ES256K", "EdDSA"],
        //    "access_token_encryption_alg_values_supported": ["ECDH-ES"],
        //    "access_token_encryption_enc_values_supported": ["A128GCM", "A256GCM"],
        //    "jwks_uri": "https://api.ebsi.xyz/did/1.0/identifiers/did:ebsi:0x416e6e6162656c2e4c65652e452d412d506f652e"
        //  },
        //  "claims": {
        //    "id_token": {
        //      "verified_claims": []
        //    }
        //  }
        //}
        ///////////////////////////////////////////////////////////////////////////
        val didAuthRequest = TrustedIssuerClient.getCredentials(false)
        println("6. QR, URI, ...")
        println("9. Trigger Wallet (Scan QR, enter URI, ...)")

        ///////////////////////////////////////////////////////////////////////////
        // The User Wallet validates the request
        ///////////////////////////////////////////////////////////////////////////
        UserWalletService.validateDidAuthRequest(didAuthRequest)
        println("11. DID-Auth request")
        println("12. Consent")

        ///////////////////////////////////////////////////////////////////////////
        // The User Wallet generates furthermore the DID Auth response:
        // Header:
        // {
        //  "alg": "ES256K",
        //  "typ": "JWT",
        //  "kid": "{DID key JWK goes here}",
        //}
        //
        // Body:
        // {
        //  "iss": "did:ebsi:0x123abc",
        //  "sub": "{thumbprint of the sub_jwk}",
        //  "aud": "{did:ebsi:RP-did-here}",
        //  "iat": 1610714000,
        //  "exp": 1610714900,
        //  "sub_jwk":{signing JWK},
        //  "sub_did_verification_method_uri": "{DID key JWK goes here}",
        //  "nonce": "n-0S6_WzA2M",
        //  "claims": {
        //    "verified_claims": {Authentication-Response-Verifiable-Presentation},
        //    "encryption_key": {JWK encryption key}
        //  }
        //}
        val didAuthResp = UserWalletService.didAuthResponse(didAuthRequest)
        println(didAuthResp)
        println("17 VC requested successfully")
        println("20 Process completed successfully")

        ///////////////////////////////////////////////////////////////////////////
        // Finally, the mutual authenticated session is established and the VC may be
        // obtained.
        ///////////////////////////////////////////////////////////////////////////
        val credential =
            TrustedIssuerClient.getCredentials(true) // user is authenticated (VC token is received); TODO: Align with spec, as the request goes to the EWallet there
        println(credential)
        println("21 Credential received")
    }

    fun vcExchange() {
        println("ESSIF Verifiable Credential Exchange from an Natural Person (Holder) to a Legal Entity")

        ///////////////////////////////////////////////////////////////////////////
        // The Relying Party must provide the Authentication Request (QR, URI, ...)
        //
        // The Auth Request contains requirements regarding the requested credential,
        // used endpoints and cipher suite. Following is an example of the payload:
        //
        // {
        //  "scope": "openid did_authn",
        //  "iss": "did:ebsi:0x416e6e6162656c2e4c65652e452d412d506f652e",
        //  "response_type": "id_token",
        //  "client_id": "<redirect-uri>",
        //  "nonce": "<random-nonce>",
        //  "registration": {
        //    "redirect_uris": ["https://app.ebsi.xyz"],
        //    "response_types": "id_token",
        //    "id_token_signed_response_alg": ["RS256", "ES256", "ES256K", "EdDSA"],
        //    "request_object_signing_alg": ["RS256", "ES256", "ES256K", "EdDSA"],
        //    "access_token_signing_alg": ["RS256", "ES256", "ES256K", "EdDSA"],
        //    "access_token_encryption_alg_values_supported": ["ECDH-ES"],
        //    "access_token_encryption_enc_values_supported": ["A128GCM", "A256GCM"],
        //    "jwks_uri": "https://api.ebsi.xyz/did/1.0/identifiers/did:ebsi:0x416e6e6162656c2e4c65652e452d412d506f652e"
        //  },
        //  "claims": {
        //    "id_token": {
        //      "verified_claims": {
        //        "verification": {
        //          "trust_framework": "EBSI",
        //          "evidence": {
        //            "type": {
        //              "value": "verifiable_credential"
        //            },
        //            "document": {
        //              "type": {
        //                "essential": true,
        //                "value": ["VerifiableCredential", "VerifiableAuthorisation"]
        //              },
        //              "credentialSchema": {
        //                "id": {
        //                  "essential": true,
        //                  "value": "https://ebsi.xyz/trusted-schemas-registry/verifiable-authorisation"
        //                }
        //              }
        //            }
        //          }
        //        }
        //      }
        //    }
        //  }
        //}
        ///////////////////////////////////////////////////////////////////////////
        println("1 Request Login")
        val vcExchangeRequest = RelyingParty.signOn()
        println("6. QR, URI")

        ////////////////////////////////////////////////////////////////////////////
        // The User Wallet generates the Authentication Response including the requested
        // Verifiable Presentation in order to initiate an authenticated session:
        //
        // Response Header:
        // {
        //  "alg": "ES256K",
        //  "typ": "JWT",
        //  "kid": "{DID key JWK goes here}",
        //}
        //
        // Response Body:
        //{
        //  "iss": "did:ebsi:0x123abc",
        //  "sub": "{thumbprint of the sub_jwk}",
        //  "aud": "{did:ebsi:RP-did-here}",
        //  "iat": 1610714000,
        //  "exp": 1610714900,
        //  "sub_jwk":{signing JWK},
        //  "sub_did_verification_method_uri": "{DID key JWK goes here}",
        //  "nonce": "n-0S6_WzA2M",
        //  "claims": {
        //    "verified_claims": {Authentication-Response-Verifiable-Presentation},
        //    "encryption_key": {JWK encryption key}
        //  }
        //}
        UserWalletService.vcAuthResponse(vcExchangeRequest)
        println("15. Credentials share successfully")

        RelyingParty.getSession("sessionId")
        println("18. Process completed successfully")

    }

    fun createTimestamp(did: String, ethKeyAlias: String? = null, data: String): String {
        return WaltIdTimestampService().createTimestamp(did, ethKeyAlias ?: did, data)
    }

    fun getByTimestampId(timestampId: String): Timestamp? {
        return WaltIdTimestampService().getByTimestampId(timestampId)
    }

    fun getByTransactionHash(transactionHash: String): Timestamp? {
        return WaltIdTimestampService().getByTransactionHash(transactionHash)
    }
}
