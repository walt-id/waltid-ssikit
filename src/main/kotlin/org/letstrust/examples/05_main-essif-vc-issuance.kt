import org.letstrust.s.essif.EosService
import org.letstrust.services.essif.UserWalletService

fun main() {

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
