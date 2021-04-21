package org.letstrust.services.essif

import mu.KotlinLogging
import org.letstrust.services.essif.mock.RelyingParty

private val log = KotlinLogging.logger {}

object EssifFlowRunner {

    // https://ec.europa.eu/cefdigital/wiki/display/BLOCKCHAININT/2.+Main+Flow%3A+VC-Request+-+Onboarding+Flow
    fun onboard() {
        ///////////////////////////////////////////////////////////////////////////
        // Prerequisite: The LE must be authenticated and authorized by the classical
        // way before triggering the ESSIF onboarding flow.
        ///////////////////////////////////////////////////////////////////////////

        println("ESSIF onboarding of a Legal Entity by requesting a Verifiable ID")

        ///////////////////////////////////////////////////////////////////////////
        // LE requests the credential URI from the ESSIF Onboarding Service (EOS)
        ///////////////////////////////////////////////////////////////////////////
        println("1 Request V.ID (manually)")
        val credentialRequestUri = EosService.requestCredentialUri()
        log.debug { "credentialRequest: $credentialRequestUri" }

        ///////////////////////////////////////////////////////////////////////////
        // The EnterpriseWalletService processes the credentialRequestUri and forwards
        // the credential request ([POST] /credentials with and empty body) to the EOS
        ///////////////////////////////////////////////////////////////////////////
        println("3 Trigger Wallet")
        val didOwnershipReq = EnterpriseWalletService.requestVerifiableCredential(credentialRequestUri)

        ///////////////////////////////////////////////////////////////////////////
        // The DID ownership request is a JWT, which must be verified bey the public key of the EOS.
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
        log.debug { "didOwnershipReq: $didOwnershipReq" }
        println("6. Notify DID ownership request")
        println("7. Create a new DID")

        ///////////////////////////////////////////////////////////////////////////
        // Creation of DID sub-flow (see: 04_main-essif-register-did.kt)
        ///////////////////////////////////////////////////////////////////////////
        val didOfLegalEntity = EnterpriseWalletService.createDid()

        ///////////////////////////////////////////////////////////////////////////
        // Once the DID is created, the DID ownership response composed and set to the EOS
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

        val verifiableId = EnterpriseWalletService.getVerifiableCredential(didOwnershipReq, didOfLegalEntity)

        ///////////////////////////////////////////////////////////////////////////
        // The requested V.ID is returned and should look like the following::
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

        log.debug { "verifiableId: $verifiableId" }
        println("14. Successful process")

    }

    // https://ec.europa.eu/cefdigital/wiki/display/BLOCKCHAININT/2.+Authorization+API
    fun authApi() {

        println("ESSIF Authorization API")

        // Verifiable Authorization must be previously installed via ESSIF onboarding flow (DID registration)
        val verifiableAuthorization = "{\n" +
                "  \"@context\": [\n" +
                "    \"https://www.w3.org/2018/credentials/v1\"\n" +
                "  ],\n" +
                "  \"id\": \"did:ebsi-eth:00000001/credentials/1872\",\n" +
                "  \"type\": [\n" +
                "    \"VerifiableCredential\",\n" +
                "    \"VerifiableAuthorization\"\n" +
                "  ],\n" +
                "  \"issuer\": \"did:ebsi:000001234\",\n" +
                "  \"issuanceDate\": \"2020-08-24T14:13:44Z\",\n" +
                "  \"expirationDate\": \"2020-08-25T14:13:44Z\",\n" +
                "  \"credentialSubject\": {\n" +
                "    \"id\": \"did:key:z6MksTeZpzyCdeRHuvk6kAAfQQCas3NPTRtxnB5a68mDrps5\",\n" +
                "    \"hash\": \"e96e3fecdbdf2126ea62e7c6...04de0f177e5971c27dedd0d17bc649a626ac\"\n" +
                "  },\n" +
                "  \"proof\": {\n" +
                "    \"type\": \"EcdsaSecp256k1Signature2019\",\n" +
                "    \"created\": \"2020-08-24T14:13:44Z\",\n" +
                "    \"proofPurpose\": \"assertionMethod\",\n" +
                "    \"verificationMethod\": \"did:ebsi-eth:000001234#key-1\",\n" +
                "    \"jws\": \"eyJhbGciOiJSUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..TCYt5X\"\n" +
                "  }\n" +
                "}\n"

        ///////////////////////////////////////////////////////////////////////////
        // Run authentication protocol (DID Auth + Authenticated Key Exchange Protocol)
        // and receive JWT Access Token + Authenticated Key Exchange Protocol.
        ///////////////////////////////////////////////////////////////////////////

        val accessToken =  UserWalletService.requestAccessToken(verifiableAuthorization)

        ///////////////////////////////////////////////////////////////////////////
        // Protected resource can now be accessed
        ///////////////////////////////////////////////////////////////////////////

        UserWalletService.accessProtectedResource(accessToken) // e.g updateDID, revoke VC
    }

    // https://ec.europa.eu/cefdigital/wiki/display/BLOCKCHAININT/VC-Issuance+Flow
    fun vcIssuance() {
        println("Credential issuance from an Legal Entity (EOS/Trusted Issuer) to a Natural Person.")

        ///////////////////////////////////////////////////////////////////////////
        // Prerequisite: The Natural Person (NP) must be authenticated and authorized by the
        // classical method of the Trusted Issuer before triggering the credential issuance flow
        ///////////////////////////////////////////////////////////////////////////

        println("1 Request VC (Manually)")

        ///////////////////////////////////////////////////////////////////////////
        // The ESSIF Onboarding Service / Trusted Issuer generates the DI-Auth Request.
        // The request looks as follows:
        //
        // {
        //    "uri": "openid://?response_type=id_token&client_id=https%3A%2F%2Frp.example.com%2Fcb&scope=openid%20did_authn&request=<authentication-request-JWS>",
        //    "callback": "https://ti.example.com/callback"
        //}
        // whereas the JWT rew header contains the DID key of the issuer:
        // {
        //  "alg": "ES256K",
        //  "typ": "JWT",
        //  "jwk": "{DID key of the issuer}",
        // }
        // and the JWT body is the following structure:
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
        val didAuthRequest = EosService.getCredentials()
        println("6. QR, URI, ...")
        println("9. Trigger Wallet (Scan QR, enter URI, ...)")

        ///////////////////////////////////////////////////////////////////////////
        // The User Wallet validates the request
        ///////////////////////////////////////////////////////////////////////////
        UserWalletService.validateDidAuthRequest(didAuthRequest)
        println("11. DID-Auth request")
        println("12. Consent")

        ///////////////////////////////////////////////////////////////////////////
        // The User Wallet generates furthermore the DID-Auth response:
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

        println("17 VC requested successfully")
        println("20 Process completed successfully")
        ///////////////////////////////////////////////////////////////////////////
        // Finally the mutual authenticated session is established and the VC may be
        // obtained.
        ///////////////////////////////////////////////////////////////////////////
        val credential = EosService.getCredentials(true) // user is authenticated (VC token is received); TODO: Align with spec, as the request goes to the EWallet there
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
