package org.letstrust.examples

import mu.KotlinLogging
import org.letstrust.services.essif.EnterpriseWalletService
import org.letstrust.services.essif.mock.DidRegistry

private val log = KotlinLogging.logger {}

fun main() {

    ///////////////////////////////////////////////////////////////////////////
    // Note, that this is a sub-flow of the main-onboarding flow (see: 03_main-essif-onboarding.kt)
    // Total flow is covered by EnterpriseWalletService::createDid()
    ///////////////////////////////////////////////////////////////////////////

    println("ESSIF DID-registration")

    ///////////////////////////////////////////////////////////////////////////
    // Generate ETH address + DID
    ///////////////////////////////////////////////////////////////////////////

    val did = EnterpriseWalletService.didGeneration()

    ///////////////////////////////////////////////////////////////////////////
    // Obtain Verifiable Authorization by following steps
    // - Precondition: Access Token needs to be obtained via EU login (handled by parent flow)
    // - Proof control over DID:
    //
    // Request-JWT-Header:
    //{
    //  "alg": "ES256K",
    //  "typ": "JWT",
    //  "jwk": "{EOS-DID Key}",
    //}
    // Request-JWT-Body:
    // {
    //  "id": "<random-id>",
    //  "iss": "did:ebsi:2123456789AB...fghijkmno",
    //  "callback": "<callback-url>"
    //  "nonce": "<random-nonce>",
    //}
    //
    // Response-JWT-Header:
    // {
    //  "alg": "ES256K",
    //  "typ": "JWT",
    //  "jwk": "{User Wallet-DID Key}",
    //}
    // Response-JWT-Body:
    // {
    //  "nonce": "<request-nonce>"
    //  "data": {
    //    "@context": [
    //      "https://w3.org/ns/did/v1,
    //      https: //ebsi.org/ns/did/v1"
    //    ],
    //    "id": "did:ebsi:2123456789AB...fghijkmno",
    //    "verificationMethod": [{
    //        "id": "did:ebsi:2123456789AB...fghijkmno#key-1",
    //        "type": "Ed25519VerificationKey2018",
    //        "controller": "did:ebsi:2123456789AB...fghijkmno",
    //        "publicKeyBase58": "5F5Q1qJbbvcyhnStAmkjF5rUBKe1XnHJHt2Tj49tTDpW"
    //      },
    //      {
    //        "id": "did:ebsi:2123456789AB...fghijkmno#key-2",
    //        "type": "X25519KeyAgreementKey2019",
    //        "controller": "did:ebsi:2123456789AB...fghijkmno",
    //        "publicKeyBase58": "GnxxE5rAFYoRS6af7XArmzh7gNS68XTAKjhNDRsfhrFJ"
    //      },
    //      {
    //        "id": "did:ebsi:2123456789AB...fghijkmno#key-3",
    //        "type": "EidasVerificationKey2021",
    //        "controller": "did:ebsi:2123456789AB...fghijkmno",
    //        "publicKeyPem": "-----BEGIN.."
    //      }
    //    ],
    //    "authentication": [
    //      "did:ebsi:2123456789AB...fghijkmno#key-3"
    //    ],
    //    "assertionMethod": [
    //      "did:ebsi:2123456789AB...fghijkmno#key-1"
    //    ],
    //    "keyAgreement": [
    //      "did:ebsi:2123456789AB...fghijkmno#key-2"
    //    ],
    //    "proof": {
    //      "type": "Ed25519Signature2018",
    //      "created": "2019-06-22T14:11:44Z",
    //      "proofPurpose": "assertionMethod",
    //      "verificationMethod": "did:ebsi:2123456789AB...fghijkmno#key-1",
    //      "jws": "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
    //    }
    //  }
    //}
    // - Receive/store Verifiable Authorisation
    ///////////////////////////////////////////////////////////////////////////

    val verifiableAuthoriztation = EnterpriseWalletService.requestVerifiableAuthorization(did)
    log.debug { "verifiableAuthoriztation: $verifiableAuthoriztation" }


    ///////////////////////////////////////////////////////////////////////////
    // Inserting DID to the ledger
    ///////////////////////////////////////////////////////////////////////////
    val unsignedTransaction = DidRegistry.insertDidDocument()
    println("16. [EWallet] 200 <unsigned transaction>")
    println("17. [EWallet] Generate <signed transaction>")
    val signedTransaction = ""
    DidRegistry.signedTransaction(signedTransaction)

}
