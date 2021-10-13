package id.walt.services.essif

import mu.KotlinLogging
import id.walt.common.readEssifBearerToken
import id.walt.services.WaltIdServices
import id.walt.services.context.WaltContext
import id.walt.services.essif.didebsi.DidEbsiService
import id.walt.services.essif.enterprisewallet.EnterpriseWalletService
import id.walt.services.essif.mock.RelyingParty
import id.walt.services.essif.userwallet.UserWalletService
import id.walt.services.hkvstore.HKVKey
import id.walt.services.hkvstore.HKVStoreService
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

        ///////////////////////////////////////////////////////////////////////////
        // Requesting the DID Auth Request from the ESSIF Onboarding Service (EOS)
        ///////////////////////////////////////////////////////////////////////////

        log.debug { "Requesting a Verifiable ID from ESSIF Onboarding Service (EOS)" }

        val authRequestResponse = LegalEntityClient.eos.authenticationRequests()

        log.debug { "AuthRequestResponse:\n$authRequestResponse" }

        val didAuthRequest = enterpriseWalletService.parseDidAuthRequest(authRequestResponse)

        log.debug { "DidAuthRequest:\n$didAuthRequest" }

        ///////////////////////////////////////////////////////////////////////////
        // Constructing and returning the DID Auth Response
        ///////////////////////////////////////////////////////////////////////////

        val idToken =
            enterpriseWalletService.constructAuthResponseJwt(did, didAuthRequest.client_id, didAuthRequest.nonce)

        val verifiableAuthorization = LegalEntityClient.eos.authenticationResponse(idToken, bearerToken)

        ///////////////////////////////////////////////////////////////////////////
        // Storing the received Verifiable Authorization
        ///////////////////////////////////////////////////////////////////////////

        log.debug { "Verifiable Authorization received:\n${verifiableAuthorization}" }


        WaltContext.hkvStore.put(HKVKey("ebsi", did.substringAfterLast(":"), verifiableAuthorizationFile), verifiableAuthorization)


        ///////////////////////////////////////////////////////////////////////////
        // The EnterpriseWalletService processes the credentialRequestUri and forwards
        // the credential request ([POST] /credentials with and empty body) to the EOS
        ///////////////////////////////////////////////////////////////////////////
//        println("3 Trigger Wallet")
//        val didOwnershipReq = EnterpriseWalletService.requestVerifiableCredential(authResponse)

        ///////////////////////////////////////////////////////////////////////////
        // The DID ownership request is a JWT, which is verified by the public key of the EOS.
        //
        // Header:
        // {
        //  "alg": "ES256K",
        //  "typ": "JWT",
        //  "jwk": "{DID of the EOS}"
        //}
        //
        // The body looks as follows:
        // {
        //  "id": "<random-id>",
        //  "type": "credential-request",
        //  "iss": "did:ebsi:2123456789AB...fghijkmno",
        //  "callback": "<callback-url>",
        //  "nonce": "<random-nonce>",
        //  "claims": {
        //    "types": [
        //      {
        //        "id": "https://essif.europa.eu/tsr-vid/verifiableid1.json",
        //        "type": "JsonSchemaValidator2018"
        //      }
        //    ]
        //  }
        //}
        ///////////////////////////////////////////////////////////////////////////
//        log.debug { "didOwnershipReq: $didOwnershipReq" }
//        println("6. Notify DID ownership request")
//        println("7. Create a new DID")
//
//        ///////////////////////////////////////////////////////////////////////////
        // Creation of DID sub-flow (see: 04_main-essif-register-did.kt)
        ///////////////////////////////////////////////////////////////////////////
        //val didOfLegalEntity = EnterpriseWalletService.createDid()

        ///////////////////////////////////////////////////////////////////////////
        // Once the DID is created, the DID ownership response is composed and sent to the EOS.
        //
        // The DID of the Legal Entity is placed in the header:
        // {
        //  "alg": "ES256K",
        //  "typ": "JWT",
        //  "jwk": "{DID of LE}"
        //}
        //
        // The body returns the request-nonce among
        // {
        //  "nonce": "<request-nonce>",
        //  "type": "credential-response",
        //  "claims": {
        //    "types": [
        //      {
        //        "id": "https://essif.europa.eu/tsr-vid/verifiableid1.json",
        //        "type": "JsonSchemaValidator2018"
        //      }
        //    ]
        //  }
        //}
        ///////////////////////////////////////////////////////////////////////////
        //      val verifiableId = EnterpriseWalletService.getVerifiableCredential(didOwnershipReq, didOfLegalEntity)

        ///////////////////////////////////////////////////////////////////////////
        // The requested V.ID is returned and should look like the following:
        // {
        //   "@context": [
        //     "https://www.w3.org/2018/credentials/v1",
        //     "https://essif.europa.eu/schemas/v-id/2020/v1",
        //     "https://essif.europa.eu/schemas/eidas/2020/v1"
        //   ],
        //   "id": "identity#verifiableID#1dee355d-0432-4910-ac9c-70d89e8d674e",
        //   "type": [
        //     "VerifiableCredential",
        //     "VerifiableID"
        //   ],
        //   "issuer": "did:ebsi:2123456789AB...fghijkmno",
        //   "issuanceDate": "2019-06-22T14:11:44Z",
        //   "validFrom": "2019-06-22T14:11:44Z",
        //   "credentialSubject": {
        //     "id": "did:ebsi:2123456789AB...fghijkmno",
        //     "legalPersonalIdentifier": "123456789",
        //     "legalName": "Example Company",
        //   },
        //   "credentialStatus": {
        //     "id": "https://essif.europa.eu/status/identity#verifiableID#1dee355d-0432-4910-ac9c-70d89e8d674e",
        //     "type": "CredentialStatusList2020"
        //   },
        //   "credentialSchema": {
        //     "id": "https://essif.europa.eu/tsr-vid/verifiableid1.json",
        //     "type": "JsonSchemaValidator2018"
        //   },
        //   "evidence": [{
        //     "id": "https://essif.europa.eu/tsr-vid/evidence/f2aeec97-fc0d-42bf-8ca7-0548192d4231",
        //     "type": ["DocumentVerification"],
        //     "verifier": "did:ebsi:2123456789AB...fghijkmno",
        //     "evidenceDocument": "Passport",
        //     "subjectPresence": "Physical",
        //     "documentPresence": "Physical"
        //   }],
        //   "proof": {
        //     "type": "EidasSeal2021",
        //     "created": "2019-06-22T14:11:44Z",
        //     "proofPurpose": "assertionMethod",
        //     "verificationMethod": "did:ebsi:2123456789AB...fghijkmno#1088321447",
        //     "jws": "BD21J4fdlnBvBA+y6D...fnC8Y="
        //   }
        //}
        ///////////////////////////////////////////////////////////////////////////
//
//        log.debug { "verifiableId: $verifiableId" }
//        println("14. Successful process")

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

        WaltContext.hkvStore.put(HKVKey("ebsi", did.substringAfterLast(":"), ebsiAccessTokenFile), accessToken)

        ///////////////////////////////////////////////////////////////////////////
        // Protected resource can now be accessed
        ///////////////////////////////////////////////////////////////////////////

        //UserWalletService.accessProtectedResource(accessToken) // e.g updateDID, revoke VC
    }

    fun registerDid(did: String, ethKeyAlias: String) {
        val maxTries = 3
        for (i in 1..maxTries) {
            try {
                didEbsiService.registerDid(did, ethKeyAlias)
                break
            }catch (e: Exception) {
                log.debug { "Trying register DID EBSI failed (fail count: $i)" }
                log.debug { e }
                if (i == maxTries) {
                    throw e
                }
            }
        }
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
        // Finally the mutual authenticated session is established and the VC may be
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

}
