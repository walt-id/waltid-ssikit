package org.letstrust.essif

import org.junit.Test
import org.letstrust.services.essif.UserWalletService

class VcExchangeFlowTest {

    val rp = RelyingParty()

    @Test
    fun testVcExchangeFlow() {

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
        val vcExchangeRequest = rp.signOn()
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

        rp.getSession("sessionId")
        println("18. Process completed successsfully")
    }
}
